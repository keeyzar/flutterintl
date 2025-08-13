package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.FileBestGuessContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.SingleLiteral
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service.PsiElementIdReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.psiutils.PsiElementIdGenerator
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.ProgrammerException
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.ContextProvider
import java.util.*

class IdeaGatherBestGuessContext(
    private val contextProvider: ContextProvider,
    private val psiElementIdGenerator: PsiElementIdGenerator,
    private val psiElementReferenceContainer: PsiElementIdReferenceProvider,
) : GatherBestGuessContext {

    override fun fromMultipleFiles(processUUID: UUID, files: List<PsiFile>): FileBestGuessContext? {
        TODO("Not yet implemented")
    }

    override fun fromPsiElements(processUUID: UUID, elements: List<PsiElement>): FileBestGuessContext? {
        if (elements.isEmpty()) {
            return null
        }
        val context = contextProvider.getAutoLocalizeContext(processUUID)
            ?: throw ProgrammerException("The programmer forgot to set the context, or there was some other strange mystery. Anyways, we did not find the context for the process with id: '$processUUID'")

        val stringLiterals = elements
            .fold(ArrayList<SingleLiteral>()) { stringLiterals, psiElement ->
                val id = psiElementIdGenerator.createIdFromPsiElement(psiElement)
                psiElementReferenceContainer.putElement(id, psiElement)
                stringLiterals += SingleLiteral(id, psiElement.text)
                stringLiterals
            }


        return FileBestGuessContext(
            filename = context.baseFile.name,
            literals = stringLiterals
        )
    }
}
