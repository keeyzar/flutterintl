package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.lang.dart.DartTokenTypes
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.exception.PsiElementException

class FlutterPsiService {
    fun getStringFromDartLiteral(psiElement: PsiElement): String {
        return when (psiElement.elementType) {
            DartTokenTypes.REGULAR_STRING_PART -> removeSurroundingQuotes(psiElement)
            DartTokenTypes.OPEN_QUOTE -> removeSurroundingQuotes(psiElement)
            DartTokenTypes.CLOSING_QUOTE -> removeSurroundingQuotes(psiElement)
            else -> throw PsiElementException("PsiElement is not a string literal, it's a ${psiElement.elementType}, text: ${psiElement.text}")
        }
    }

    fun getParentFromDartLiteral(psiElement: PsiElement): PsiElement {
        return when (psiElement.elementType) {
            DartTokenTypes.REGULAR_STRING_PART -> psiElement.parent
            DartTokenTypes.OPEN_QUOTE -> psiElement.parent
            DartTokenTypes.CLOSING_QUOTE -> psiElement.parent
            else -> throw PsiElementException("PsiElement is not a string literal, it's a ${psiElement.elementType}, text: ${psiElement.text}")
        }
    }

    private fun removeSurroundingQuotes(psiElement: PsiElement) = psiElement.parent.text
        .removeSurrounding("\"\"\"")
        .removeSurrounding("\"")
        .trim()

    fun isDartString(element: PsiElement): Boolean {
        return element.elementType == DartTokenTypes.REGULAR_STRING_PART
                || element.elementType == DartTokenTypes.OPEN_QUOTE
                || element.elementType == DartTokenTypes.CLOSING_QUOTE
    }
}
