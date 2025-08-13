package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.FileBestGuessContext
import java.util.*

interface GatherBestGuessContext {
    /**
     * Gathers the context from multiple files, usually without user interaction for string selection.
     * @return null, if no relevant literals are found.
     */
    fun fromMultipleFiles(processUUID: UUID, files: List<PsiFile>): FileBestGuessContext?

    fun fromPsiElements(processUUID: UUID, elements: List<PsiElement>): FileBestGuessContext?
}
