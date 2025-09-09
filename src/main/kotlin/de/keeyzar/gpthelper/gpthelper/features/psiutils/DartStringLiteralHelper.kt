package de.keeyzar.gpthelper.gpthelper.features.psiutils

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.descendantsOfType
import com.jetbrains.lang.dart.psi.DartAdditiveExpression
import com.jetbrains.lang.dart.psi.DartCallExpression
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import de.keeyzar.gpthelper.gpthelper.features.psiutils.filter.DartStringLiteralFilter

/**
 * in a given PsiFile find all the DartStringLiterals
 */
class DartStringLiteralHelper(
    private val dartAdditiveExpressionExtractor: DartAdditiveExpressionExtractor,
    private val dartStringLiteralFilters: List<DartStringLiteralFilter>,
) {

    /**
     * this PsiElement is either a [DartStringLiteralExpression] or a [DartAdditiveExpression]
     * @return a map of the psi element and a boolean, whether it should be preselected or not
     */
    fun findStringPsiElements(psiFile: PsiFile): Map<PsiElement, Boolean> {
        //I bet there are some easier ways to do this. e.g. TreeUtils, get all string literals, get all additive expressions, only take additive expressions with children of the type string literal
        //and then remove all string literals, which are already part of the additive expressions (child)
        //anyway, seems to work for now

        //we have a list of filters, because over time we want to add more and more stuff to automatically be removed
        return ReadAction.compute<Map<PsiElement, Boolean>, Throwable> {
            val elements = findLiterals(psiFile)
                .map { dartAdditiveExpressionExtractor.extractAdditiveExpression(it) ?: it }
                .filter { stringLiteral ->
                    dartStringLiteralFilters.all { literalFilter ->
                        literalFilter.filter(stringLiteral)
                    }
                }.toSet()

            elements.associateWith { !isInsidePrintOrLogStatement(it) }
        }
    }

    private fun isInsidePrintOrLogStatement(element: PsiElement): Boolean {
        val callExpression = PsiTreeUtil.getParentOfType(element, DartCallExpression::class.java) ?: return false
        val referenceExpression = callExpression.expression ?: return false
        val functionName = referenceExpression.text
        val listOfFunctions = listOf("print", "log", "debugPrint", "debugPrintStack")
        return listOfFunctions.contains(functionName)
    }

    /**
     * a string with "You have $credits Credits" might provide only part of the string
     */
    fun getWholeStringExpression(psiElement: PsiElement) {

    }

    fun findLiterals(psiFile: PsiFile): List<DartStringLiteralExpression> {
        return psiFile.descendantsOfType<DartStringLiteralExpression>().toList()
    }
}
