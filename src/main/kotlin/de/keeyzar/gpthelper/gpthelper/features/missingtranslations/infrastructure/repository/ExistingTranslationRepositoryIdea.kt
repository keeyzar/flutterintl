package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.infrastructure.repository

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.openapi.application.ApplicationManager
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.ExistingTranslation
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.repository.ExistingTranslationRepository
import de.keeyzar.gpthelper.gpthelper.features.psiutils.arb.ArbPsiUtils
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import java.nio.file.Path

class ExistingTranslationRepositoryIdea(
    private val translationFileRepository: TranslationFileRepository,
    private val arbPsiUtils: ArbPsiUtils
) : ExistingTranslationRepository<PsiElement> {
    override fun getExistingTranslation(reference: PsiElement, baseLanguage: Language, key: String): ExistingTranslation? {
        val path = translationFileRepository.getPathToFile(baseLanguage)
        val psiFile: PsiFile = getPsiFileFromPath(reference, path) ?: return null
        //alright, we can now get the corresponding entry
        return arbPsiUtils.getArbEntryFromKeyOnRootObject(psiFile, key)?.let {
            return ExistingTranslation(
                key = key,
                value = it.value,
                description = it.description
            )
        }
    }

    private fun getPsiFileFromPath(element: PsiElement, path: Path): PsiFile? {
        val virtualFile = LocalFileSystem.getInstance().findFileByIoFile(path.toFile())
        return virtualFile?.let {
            ApplicationManager.getApplication().runReadAction<PsiFile?> {
                PsiManager.getInstance(element.project).findFile(it)
            }
        }
    }
}
