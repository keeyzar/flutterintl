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
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.service.L10NContentService

/**
 * will fix the import in the current file
 */
class ImportFixer(private val l10NContentService: L10NContentService) {
    fun addTranslationImportIfMissing(project: Project, element: PsiElement) {
        WriteCommandAction.runWriteCommandAction(project) {
            CommandProcessor.getInstance().executeCommand(project, {
                val dartFile = element as? DartFile ?: element.findParentOfType<DartFile>()

                if (dartFile != null && isImportMissing(dartFile)) {
                    val newImportStatement = createStatement(element)
                    dartFile.addBefore(newImportStatement, dartFile.firstChild)
                }
            }, "translation", "translate")
        }
    }

    internal fun createStatement(element: PsiElement): DartImportStatement {
        val project = element.project
        val importPath = getGeneratedImportPath(project, l10NContentService)
        val statement = "import '$importPath';"
        val dummyFile = DartElementGenerator.createDummyFile(project, statement)
        return PsiTreeUtil.getChildOfType(dummyFile, DartImportStatement::class.java)!!
    }

    private fun getGeneratedImportPath(project: Project, contentService: L10NContentService): String {
        val projectName = contentService.getProjectName()
        val arbDir = contentService.getPath().removePrefix("/")
        val outputLocalizationFile = contentService.getOutputLocalizationFile()

        return "package:$projectName/$arbDir/$outputLocalizationFile"
    }

    private fun isImportMissing(dartFile: DartFile): Boolean {
        val project = dartFile.project
        val contentService = L10NContentService(project)
        val importPath = getGeneratedImportPath(project, contentService)
        val imports = PsiTreeUtil.getChildrenOfTypeAsList(dartFile, DartImportStatement::class.java)
        return imports.none { it.uriString == importPath }
    }
}
