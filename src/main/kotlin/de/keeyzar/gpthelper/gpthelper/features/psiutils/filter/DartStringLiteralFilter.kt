package de.keeyzar.gpthelper.gpthelper.features.psiutils.filter

import com.intellij.psi.PsiElement

interface DartStringLiteralFilter {
    fun filter(psiElement: PsiElement): Boolean
}
