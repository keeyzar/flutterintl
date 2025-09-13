package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions

import com.intellij.patterns.PatternCondition
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartTokenTypes


class DartStringRequiredPattern : PatternCondition<PsiElement>("DartStringRequiredPattern") {
    override fun accepts(
        t: PsiElement,
        context: ProcessingContext?
    ): Boolean {
        val types = listOf(
            DartTokenTypes.ARGUMENT_LIST,
            DartTokenTypes.VAR_DECLARATION_LIST
        )

        return PsiTreeUtil.findFirstParent(t) {
            types.contains(it.elementType)
        } != null
    }

}