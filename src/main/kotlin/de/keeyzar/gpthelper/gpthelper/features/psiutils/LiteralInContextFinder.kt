package de.keeyzar.gpthelper.gpthelper.features.psiutils

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.jetbrains.lang.dart.psi.DartCallExpression
import com.jetbrains.lang.dart.psi.DartStatements
import com.jetbrains.lang.dart.psi.DartVarDeclarationList

/**
 * show a psiElement in the context of the file, i.e. show the surrounding Statement
 */
class LiteralInContextFinder(
) {
    /**
     * return either this psiElement, or some more context, if possible
     */
    fun findContext(psiElement: PsiElement): PsiElement {
        //first var declaration list, then call expression, then statement, then psiElement
        return ParentFinderChain(
            listOf(
                ParentFinderDeclarationList(),
                ParentFinderCallExpression(),
                ParentFinderStatement(),
            )
        ).find(psiElement) ?: psiElement
    }

    fun interface ParentFinder {
        fun find(psiElement: PsiElement): PsiElement?
    }

    class ParentFinderChain(private val parentFinders: List<ParentFinder>) : ParentFinder {
        override fun find(psiElement: PsiElement): PsiElement? {
            return parentFinders.firstNotNullOfOrNull { it.find(psiElement) }
        }
    }

    class ParentFinderCallExpression : ParentFinder {
        override fun find(psiElement: PsiElement): PsiElement? {
            return psiElement.parentOfType<DartCallExpression>()
        }
    }

    class ParentFinderStatement : ParentFinder {
        override fun find(psiElement: PsiElement): PsiElement? {
            return psiElement.parentOfType<DartStatements>()
        }
    }

    class ParentFinderDeclarationList : ParentFinder {
        override fun find(psiElement: PsiElement): PsiElement? {
            //if there is a declaration list, return it, otherwise return the psiElement
            return psiElement.parentOfType<DartVarDeclarationList>()
        }
    }
}
