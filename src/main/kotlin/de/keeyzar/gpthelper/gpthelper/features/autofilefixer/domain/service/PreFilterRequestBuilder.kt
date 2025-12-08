package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterRequest
import de.keeyzar.gpthelper.gpthelper.features.psiutils.SmartPsiElementWrapper

/**
 * Service to prepare pre-filter requests from PSI elements
 */
interface PreFilterRequestBuilder {
    /**
     * Builds a pre-filter request from a map of PSI elements wrapped in SmartPointers
     * @param elements Map of SmartPsiElementWrapper to their pre-selection state
     * @return PreFilterRequest ready to be sent to the AI
     */
    fun buildRequest(elements: Map<SmartPsiElementWrapper<PsiElement>, Boolean>): PreFilterRequest
}

