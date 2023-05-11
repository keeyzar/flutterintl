package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.psi.PsiElement
import com.intellij.psi.util.elementType
import com.jetbrains.lang.dart.DartTokenTypes
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.exception.PsiElementException

class FlutterPsiService {
    fun getStringFromDartLiteral(psiElement: PsiElement): String {
        return when (psiElement.elementType) {
            DartTokenTypes.REGULAR_STRING_PART -> psiElement.text
            DartTokenTypes.OPEN_QUOTE -> psiElement.nextSibling.text
            DartTokenTypes.CLOSING_QUOTE -> psiElement.prevSibling.text
            else -> throw PsiElementException("PsiElement is not a string literal, it's a ${psiElement.elementType}, text: ${psiElement.text}")
        }
    }

    fun isDartString(element: PsiElement): Boolean {
        return element.elementType == DartTokenTypes.REGULAR_STRING_PART
                || element.elementType == DartTokenTypes.OPEN_QUOTE
                || element.elementType == DartTokenTypes.CLOSING_QUOTE
    }
}
