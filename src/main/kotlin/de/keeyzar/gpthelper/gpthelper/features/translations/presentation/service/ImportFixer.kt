package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.findParentOfType
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.DartImportStatement
import com.jetbrains.lang.dart.util.DartElementGenerator
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import java.io.File
import java.nio.file.Paths

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

                if (dartFile != null && isImportMissing(dartFile, userSettings)) {
                    val newImportStatement = createStatement(element, userSettings)
                    dartFile.addBefore(newImportStatement, dartFile.firstChild)
                }
            }, "translation", "translate")
        }
    }

    internal fun createStatement(element: PsiElement, userSettings: UserSettings): DartImportStatement {
        val project = element.project
        val importPath = getGeneratedImportPath(project, userSettings)
        val statement = "import '$importPath';"
        val dummyFile = DartElementGenerator.createDummyFile(project, statement)
        return PsiTreeUtil.getChildOfType(dummyFile, DartImportStatement::class.java)!!
    }

    private fun getGeneratedImportPath(project: Project, userSettings: UserSettings): String {
        val basePath = project.basePath ?: throw IllegalStateException("Project base path is null")

        val pubspecPath = Paths.get(basePath, "pubspec.yaml").toString()
        val pubspecFile = File(pubspecPath)
        if (!pubspecFile.exists()) {
            throw IllegalStateException("pubspec.yaml not found at $pubspecPath. Cannot create import statement.")
        }
        val projectName = pubspecFile.readLines()
            .firstOrNull { it.trim().startsWith("name:") }
            ?.substringAfter("name:")
            ?.substringBefore('#')
            ?.trim()
            ?: throw IllegalStateException("Could not find 'name' in pubspec.yaml")

        // GptHelperSettings now loads arbDir and outputLocalizationFile from l10n.yaml,
        // so we can just use them from userSettings.
        //when starting with lib/
        //then we need to remove lib/
        //this is because lib is the default - we do not use this path
        //there may be different combinations, that may be an issue, but well.
        //I've not yet encountered that
        val arbDir = userSettings.arbDir?.removePrefix("lib/")

        val outputLocalizationFile = userSettings.outputLocalizationFile

        return "package:$projectName/$arbDir/$outputLocalizationFile"
    }

    private fun isImportMissing(dartFile: DartFile, userSettings: UserSettings): Boolean {
        val project = dartFile.project
        val importPath = getGeneratedImportPath(project, userSettings)
        val imports = PsiTreeUtil.getChildrenOfTypeAsList(dartFile, DartImportStatement::class.java)
        return imports.none { it.uriString == importPath }
    }
}
