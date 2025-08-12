package de.keeyzar.gpthelper.gpthelper.features.psiutils.filter

import com.intellij.psi.PsiElement
import com.intellij.psi.util.parentOfType
import com.jetbrains.lang.dart.psi.DartImportOrExportStatement

/**
 * filters importStatements
 */
class ImportStatementFilterDartString : DartStringLiteralFilter {
    override fun filter(psiElement: PsiElement): Boolean {
        psiElement.parentOfType<DartImportOrExportStatement>()?.let {
            return false
        }
        return true
    }
}
