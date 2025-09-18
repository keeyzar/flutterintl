package de.keeyzar.gpthelper.gpthelper.features.setup.infrastructure.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.DartFileType
import com.jetbrains.lang.dart.psi.*
import com.jetbrains.lang.dart.util.DartElementGenerator
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.AppReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.service.L10NContentService
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.ImportFixer

/**
 * Dummy-Implementierung, die alle .dart-Dateien unterhalb von lib nach MaterialApp/CupertinoApp durchsucht.
 */
class IdeaAppReferenceProvider(
    private val project: Project,
    private val importFixer: ImportFixer,
    private val l10nContentService: L10NContentService,
) : AppReferenceProvider {
    override fun findAppReferences(): List<Any> {
        val thingsWeWantToFind = listOf("MaterialApp", "CupertinoApp")
        val references = mutableListOf<PsiElement>()
        val dartFiles = FileTypeIndex.getFiles(DartFileType.INSTANCE, GlobalSearchScope.projectScope(project))
        for (dartFile in dartFiles) {
            val psiFile = PsiManager.getInstance(project).findFile(dartFile) as? DartFile ?: continue
            psiFile.accept(object : PsiRecursiveElementVisitor() {
                override fun visitElement(element: PsiElement) {
                    if (element is DartCallExpression) {
                        if (thingsWeWantToFind.contains(element.children.firstOrNull { it is DartReferenceExpression }?.text)) {
                            references.add(element)
                        }
                    }
                    super.visitElement(element)
                }
            })
        }
        return references.map { it as Any }
    }

    @Deprecated("Use enableLocalizationOnDummy and modifyFileContent instead")
    override fun enableLocalization(reference: Any): Boolean {
        val referenceElement = reference as? DartCallExpression ?: return false
        val modifiedContent = enableLocalizationOnDummy(referenceElement)
        if (modifiedContent != null) {
            modifyFileContent(referenceElement, modifiedContent)
            return true
        }
        return false
    }

    /**
     * Creates a dummy copy of the file containing the reference, modifies it, and returns the modified content as a string.
     * The original file is not changed.
     */
    override fun enableLocalizationOnDummy(reference: Any): String? {
        val referenceElement = reference as? DartCallExpression ?: return null
        val containingFile = referenceElement.containingFile as? DartFile ?: return null

        // 1. Create an in-memory copy of the file.
        val dummyFile = containingFile.copy() as? DartFile ?: return null

        // 2. Wrap all PSI modifications in a write action. This is mandatory.
        ApplicationManager.getApplication().runWriteAction {
            val elementInDummy = PsiTreeUtil.findSameElementInCopy(referenceElement, dummyFile)
                ?: return@runWriteAction // exit the lambda if not found

            val argumentList = elementInDummy.arguments?.argumentList ?: return@runWriteAction

            val outputClass = l10nContentService.getOutputClass()
            val localizationsDelegateArgument =
                createNamedArgument(project, "localizationsDelegates", "$outputClass.localizationsDelegates")
            if (localizationsDelegateArgument != null) {
                argumentList.add(localizationsDelegateArgument)
            }

            val supportedLocalesArgument =
                createNamedArgument(project, "supportedLocales", "$outputClass.supportedLocales")
            if (supportedLocalesArgument != null) {
                argumentList.add(supportedLocalesArgument)
            }

            // Add import to the dummy file
            importFixer.addTranslationImportIfMissing(project, elementInDummy)

            // 3. (Highly Recommended) Reformat the modified element to fix spacing and add commas.
            CodeStyleManager.getInstance(project).reformat(elementInDummy)
        }

        // 4. Now, the text of the dummy file will reflect the modifications.
        return dummyFile.text
    }

    override fun getContent(reference: Any): String? {
        return (reference as? DartCallExpression)?.containingFile?.text
    }

    /**
     * Modifies the content of the real file with the provided string.
     */
    override fun modifyFileContent(reference: Any, newContent: String) {
        val referenceElement = reference as? DartCallExpression ?: return
        // Use a WriteCommandAction here because this is a user-facing change that should be undoable.
        WriteCommandAction.runWriteCommandAction(project) {
            val document = PsiManager.getInstance(project).findViewProvider(referenceElement.containingFile.virtualFile)?.document
            document?.setText(newContent)
        }
    }

    override fun referenceHasLocalization(reference: Any): Boolean {
        val callExpr = reference as? DartCallExpression ?: return false
        val argumentList = callExpr.arguments?.argumentList ?: return false
        val text = argumentList.text
        return text.contains("localizationsDelegates")
    }

    private fun createNamedArgument(project: Project, name: String, value: String): DartNamedArgument? {
        val dummyFile = DartElementGenerator.createDummyFile(project, "void main() { dummyFunction($name: $value); }")
        val callExpression = PsiTreeUtil.findChildOfType(dummyFile, DartCallExpression::class.java)
        return PsiTreeUtil.findChildOfType(callExpression, DartNamedArgument::class.java)
    }
}