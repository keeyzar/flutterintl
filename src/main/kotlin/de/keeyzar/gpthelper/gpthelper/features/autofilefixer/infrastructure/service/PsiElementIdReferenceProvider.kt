package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service

import com.intellij.psi.PsiElement

class PsiElementIdReferenceProvider
{
    //hashmap with string id to psiElement
    private var elements: MutableMap<String, PsiElement> = mutableMapOf()

    /**
     * remember to delete the element after you are done with it, otherwise we have a memory leak.. :)
     */
    fun putElement(id: String, element: PsiElement) {
        elements[id] = element
    }
    fun deleteElement(id: String, element: PsiElement) {
        elements.remove(id)
    }

    fun getElement(id: String): PsiElement? {
        return elements[id]
    }

    /**
     * this should not be necessary, might have multiple translations in the background, and then
     * we loose the whole context, though I could theoretically map all the elements to a project (i.e. aggregated Key)
     * TODO need to fix this
     */
    fun deleteAllElements() {
        elements.clear()
    }
}
