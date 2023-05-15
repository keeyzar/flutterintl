package de.keeyzar.gpthelper.gpthelper.features.psiutils

import com.intellij.psi.PsiElement

class PsiElementIdGenerator {
    /**
     * hash the psi element and return a unique id
     * TODO: this id generation here is not really secure
     */
    fun createIdFromPsiElement(psiElement: PsiElement): String {
        return psiElement.text.hashCode().toString()
    }
}
