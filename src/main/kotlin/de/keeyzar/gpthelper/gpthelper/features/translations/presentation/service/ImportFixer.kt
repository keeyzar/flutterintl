package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentOfType
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.DartImportStatement
import com.jetbrains.lang.dart.util.DartElementGenerator
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository

/**
 * will fix the import in the current file
 */
class ImportFixer(
    private val userSettingsRepository: UserSettingsRepository
) {
    fun addTranslationImportIfMissing(project: Project, element: PsiElement) {
        val userSettings = userSettingsRepository.getSettings()
        WriteCommandAction.runWriteCommandAction(project) {
            CommandProcessor.getInstance().executeCommand(project, {
                val dartFile = element as? DartFile ?: element.findParentOfType<DartFile>()

                //refresh the file, because it might be changed by another action inbetween

                println("import missing?")
                if (isImportMissing(dartFile, userSettings)) {
                    println("looks like it!")
                    val newImportStatement = createStatement(element, userSettings)
                    dartFile?.addBefore(newImportStatement, dartFile.firstChild);
                }
            }, "translation", "translate")
        }
    }

    internal fun createStatement(element: PsiElement, userSettings: UserSettings): DartImportStatement {
        val flutterImportStatement = getFlutterImportStatement(userSettings)
        val statement = "import '$flutterImportStatement${userSettings.outputLocalizationFile}';"
        val dummyFile = DartElementGenerator.createDummyFile(element.project, statement)
        return PsiTreeUtil.getChildOfType(dummyFile, DartImportStatement::class.java)!!;
    }

    private fun getFlutterImportStatement(userSettings: UserSettings): String {
        val flutterImportStatement = userSettings.flutterImportStatement.let {
            //append / if not present
            if (it.endsWith("/")) it else "$it/"
        }
        return flutterImportStatement
    }

    private fun isImportMissing(parent: DartFile?, userSettings: UserSettings): Boolean {
        val imports = PsiTreeUtil.getChildrenOfTypeAsList(parent, DartImportStatement::class.java);
        return imports.none { it.text.contains("${getFlutterImportStatement(userSettings)}${userSettings.outputLocalizationFile}") }
    }
}
