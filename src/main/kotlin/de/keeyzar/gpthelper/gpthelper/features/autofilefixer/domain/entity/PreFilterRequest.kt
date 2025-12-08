package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity

import com.intellij.psi.PsiElement

/**
 * Request for pre-filtering string literals to determine which ones should be translated
 */
data class PreFilterRequest(
    val literals: List<PreFilterLiteral>
)

data class PreFilterLiteral(
    val id: String,
    val literalText: String,
    val context: String,
    val psiElement: PsiElement
)

