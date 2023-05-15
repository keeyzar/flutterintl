package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.model

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

class TranslateWholeFileContext(
    val baseFile: PsiFile,
    val project: Project,
) {
}
