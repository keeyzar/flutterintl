package de.keeyzar.gpthelper.gpthelper.features.psiutils

import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.lang.dart.psi.DartFile
import com.jetbrains.lang.dart.psi.DartListLiteralExpression
import com.jetbrains.lang.dart.psi.DartNewExpression
import com.jetbrains.lang.dart.psi.DartVarAccessDeclaration
import com.jetbrains.lang.dart.psi.DartVarDeclarationList

/**
 * finds const modifiers in the hierarchy upwards
 */
class DartConstModifierFinder {
    fun checkForConstExpressionsInHierarchy(element: PsiElement): PsiElement? {
        var currentElement: PsiElement? = element

        while (currentElement != null && isNotTooFarUpInTheHierarchy(currentElement)) {
            if (possibleDirectConstParent(currentElement)) {
                val psiElement = getLeafIfExisting(currentElement)
                if (psiElement != null) {
                    return psiElement
                }
            } else if (possibleIndirectConstParent(currentElement)) {
                //get varAccessDeclaration
                val varAccessDeclaration = PsiTreeUtil.findChildOfType(currentElement, DartVarAccessDeclaration::class.java)
                val psiElement = getLeafIfExisting(varAccessDeclaration)
                if (psiElement != null) {
                    return psiElement
                }
            }
            currentElement = currentElement.parent
        }
        return null
    }

    private fun getLeafIfExisting(currentElement: PsiElement?): PsiElement? {
        val constModifier = PsiTreeUtil.findChildOfType(currentElement, LeafPsiElement::class.java)
        return if (constModifier != null && constModifier.textMatches("const")) {
            return constModifier
        } else {
            null
        }
    }

    /**
     * e.g. dart file etc. we do not need to  check, might be, that we want
     */
    private fun isNotTooFarUpInTheHierarchy(currentElement: PsiElement?) = currentElement !is DartFile

    private fun possibleDirectConstParent(currentElement: PsiElement?) =
        currentElement is DartNewExpression ||
                currentElement is DartListLiteralExpression


    private fun possibleIndirectConstParent(currentElement: PsiElement?) =
        currentElement is DartVarDeclarationList

}
