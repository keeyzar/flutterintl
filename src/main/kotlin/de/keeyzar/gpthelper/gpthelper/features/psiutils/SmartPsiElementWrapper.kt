package de.keeyzar.gpthelper.gpthelper.features.psiutils

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer

/**
 * Wrapper for SmartPsiElementPointer to handle PSI elements that can be invalidated.
 * This allows PSI elements to be tracked across file modifications.
 */
data class SmartPsiElementWrapper<T : PsiElement>(
    val pointer: SmartPsiElementPointer<T>
) {
    /**
     * Get the current PSI element, or null if it has been invalidated
     * WARNING: This requires read access! Call from within a ReadAction or on EDT with read access.
     */
    val element: T?
        get() = pointer.element

    companion object {
        /**
         * Create a SmartPsiElementWrapper from a PSI element
         * WARNING: This requires read access! Call from within a ReadAction.
         */
        fun <T : PsiElement> create(project: Project, element: T): SmartPsiElementWrapper<T> {
            val pointer = SmartPointerManager.getInstance(project)
                .createSmartPsiElementPointer(element)
            return SmartPsiElementWrapper(pointer)
        }

        /**
         * Convert a map of PsiElement to SmartPsiElementWrapper map
         * WARNING: This requires read access! Call from within a ReadAction.
         */
        fun <T : PsiElement, V> wrapMap(project: Project, map: Map<T, V>): Map<SmartPsiElementWrapper<T>, V> {
            return map.mapKeys { (element, _) -> create(project, element) }
        }

        /**
         * Convert a SmartPsiElementWrapper map back to PsiElement map, filtering out invalid elements
         * WARNING: This requires read access! Call from within a ReadAction.
         */
        fun <T : PsiElement, V> unwrapMap(map: Map<SmartPsiElementWrapper<T>, V>): Map<T, V> {
            return ReadAction.compute<Map<T, V>, RuntimeException> {
                map.mapNotNull { (wrapper, value) ->
                    wrapper.element?.let { it to value }
                }.toMap()
            }
        }

        /**
         * Convert a list of PsiElements to SmartPsiElementWrapper list
         * WARNING: This requires read access! Call from within a ReadAction.
         */
        fun <T : PsiElement> wrapList(project: Project, list: List<T>): List<SmartPsiElementWrapper<T>> {
            return list.map { create(project, it) }
        }

        /**
         * Convert a SmartPsiElementWrapper list back to PsiElement list, filtering out invalid elements
         * WARNING: This requires read access! Call from within a ReadAction.
         */
        fun <T : PsiElement> unwrapList(list: List<SmartPsiElementWrapper<T>>): List<T> {
            return ReadAction.compute<List<T>, RuntimeException> {
                list.mapNotNull { it.element }
            }
        }
    }
}

