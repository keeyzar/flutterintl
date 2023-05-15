package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.dto

import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessResponseEntry

data class BestGuessWithPsiReference(
    val id: String,
    val psiElement: PsiElement,
    val bestGuess: BestGuessResponseEntry,
) {
}
