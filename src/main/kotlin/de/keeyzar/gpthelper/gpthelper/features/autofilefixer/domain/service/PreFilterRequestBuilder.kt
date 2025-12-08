package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterLiteral
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterRequest

/**
 * Service to prepare pre-filter requests from PSI elements
 */
interface PreFilterRequestBuilder {
    /**
     * Builds a pre-filter request from a map of PSI elements
     * @param elements Map of PsiElement to their pre-selection state
     * @return PreFilterRequest ready to be sent to the AI
     */
    fun buildRequest(elements: Map<PsiElement, Boolean>): PreFilterRequest
}

