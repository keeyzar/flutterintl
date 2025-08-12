package de.keeyzar.gpthelper.gpthelper.features.psiutils

import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.psi.DartAdditiveExpression
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression

class DartAdditiveExpressionExtractor {
    /**
     * find all additions in the string to the topmost additive expression
     * so that we can find Strings of type "hello" + 3 + "world"
     */
    fun extractAdditiveExpression(dartStringLiteral: DartStringLiteralExpression): DartAdditiveExpression? {
        var parent = dartStringLiteral.parent

        // Climb up to the topmost additive expression
        while (parent?.parent is DartAdditiveExpression) {
            parent = parent.parent
        }

        return if (parent is DartAdditiveExpression) {
            parent
        } else {
            null
        }
    }

    fun isDescendantOf(stringLiteral: DartStringLiteralExpression, additiveExpression: DartAdditiveExpression): Boolean {
        var parent: PsiElement? = stringLiteral.parent
        while (parent != null) {
            if (parent == additiveExpression) {
                return true
            }
            parent = parent.parent
        }
        return false
    }
}
