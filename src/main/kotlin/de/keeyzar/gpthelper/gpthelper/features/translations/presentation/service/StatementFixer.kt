package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.elementType
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.psi.DartLiteralExpression
import com.jetbrains.lang.dart.psi.DartLongTemplateEntry
import com.jetbrains.lang.dart.psi.DartShortTemplateEntry
import com.jetbrains.lang.dart.util.DartElementGenerator
import de.keeyzar.gpthelper.gpthelper.features.psiutils.DartConstModifierFinder
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository

class StatementFixer(
    private val userSettingsRepository: UserSettingsRepository,
    private val dartConstModifierFinder: DartConstModifierFinder,
    private val flutterPsiService: FlutterPsiService,
) {
    /**
     * create a statement like this
     * S.of(context).desiredKey
     * or, if nullable
     * AppLocalizations.of(context)?.desiredKey
     */
    fun fixStatement(project: Project, element: PsiElement, desiredKey: String) {
        if (userSettingsRepository.getSettings().translateAdvancedArbKeys) {
            fixStatementAdvanced(project, element, desiredKey);
            return
        }
        val userSettings = userSettingsRepository.getSettings()
        WriteCommandAction.runWriteCommandAction(project) {
            CommandProcessor.getInstance().executeCommand(project, {
                val newStatement = createStatement(element, desiredKey, userSettings)
                checkForConstExpressionsInHierarchy(element)
                replaceStatementWithNewStatement(element, newStatement)
            }, "translation", "translate")
        }
        //this caused a lot of headaches in the past, so we postpone it a little bit
        //TODO allow formatting again - currently I do not know how to get the statement, also it's bugged oftentimes..
//        try {
//            WriteCommandAction.runWriteCommandAction(project) {
//                CodeStyleManager.getInstance(project).reformat(newPsiElement)
//            }
//        } catch (e: Exception) {
//            e.printStackTrace()
//            print("failed to reformat the code..., but this should not interrupt the whole process..")
//            //sometimes this just fails, for whatever reason
//        }
    }

    private fun fixStatementAdvanced(project: Project, element: PsiElement, desiredKey: String) {
        val userSettings = userSettingsRepository.getSettings()
        WriteCommandAction.runWriteCommandAction(project) {
            CommandProcessor.getInstance().executeCommand(project, {
                var newStatement = createStatement(element, desiredKey, userSettings)
                val argumentList = appendVariablesToStatement(element)
                if(argumentList != "") {
                    newStatement = newStatement.removeSuffix(",") + argumentList + ","
                }
                checkForConstExpressionsInHierarchy(element)
                replaceStatementWithNewStatement(element, newStatement)
            }, "translation", "translate")
        }
    }

    private fun appendVariablesToStatement(element: PsiElement): String {
        //we need to add (var1, var2, ...) to the statement, if there are variables
        //the element has long template entry and short template entries, each of these having
        //either referenceExpression
        flutterPsiService

        val wholeStatement = flutterPsiService.getParentFromDartLiteral(element)
        val variables = wholeStatement.childrenOfType<PsiElement>().stream().filter { e ->
            e is DartShortTemplateEntry || e is DartLongTemplateEntry
        }
            .map { e ->
                e.text.removePrefix("\${").removePrefix("$").removeSuffix("}")
            }
            .toList()

        return if (variables.isEmpty()) {
            "" //fine, there are no variables in the statement
        } else {
            "(${variables.joinToString(", ")})"
        }
    }

    /**
     * otherwise our code is not correct anymore. It's never const anymore.
     */
    private fun checkForConstExpressionsInHierarchy(element: PsiElement) {
        val constModifier = dartConstModifierFinder.checkForConstExpressionsInHierarchy(element)
        constModifier?.delete()
    }

    private fun replaceStatementWithNewStatement(element: PsiElement, newStatement: String): PsiElement {
        val keyAccess = DartElementGenerator.createStatementFromText(element.project, newStatement)
        //based on what the element is we need to replace either the parent or the element itself
        return when (element.elementType) {
            DartTokenTypes.REGULAR_STRING_PART -> element.parent.replace(keyAccess!!)
            DartTokenTypes.OPEN_QUOTE -> element.parent.replace(keyAccess!!)
            DartTokenTypes.CLOSING_QUOTE -> element.parent.replace(keyAccess!!)
            else -> element.replace(keyAccess!!)
        }
    }

    private fun createStatement(element: PsiElement, desiredKey: String, userSettings: UserSettings): String {
        val nullableGetter = when (userSettings.nullableGetter) {
            true -> "!"
            false -> ""
        }
        val outputClass = userSettings.outputClass
        val contextName = flutterPsiService.findBuildContextName(element) ?: "context"

        return "$outputClass.of($contextName)$nullableGetter.$desiredKey,"
    }


}
