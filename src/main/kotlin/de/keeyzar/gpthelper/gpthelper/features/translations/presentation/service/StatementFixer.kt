package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.elementType
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.util.DartElementGenerator
import de.keeyzar.gpthelper.gpthelper.features.psiutils.DartConstModifierFinder
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository

class StatementFixer(
    private val userSettingsRepository: UserSettingsRepository,
    private val dartConstModifierFinder: DartConstModifierFinder,
) {
    /**
     * create a statement like this
     * S.of(context).desiredKey
     * or, if nullable
     * AppLocalizations.of(context)?.desiredKey
     */
    fun fixStatement(project: Project, element: PsiElement, desiredKey: String) {
        val userSettings = userSettingsRepository.getSettings()
        WriteCommandAction.runWriteCommandAction(project) {
            CommandProcessor.getInstance().executeCommand(project, {
                val newStatement = createStatement(desiredKey, userSettings)
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

    private fun createStatement(desiredKey: String, userSettings: UserSettings): String {
        val nullableGetter = when (userSettings.nullableGetter) {
            true -> "!"
            false -> ""
        }
        val outputClass = userSettings.outputClass

        return "$outputClass.of(context)$nullableGetter.$desiredKey,"
    }


}
