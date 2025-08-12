package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.model

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile

data class AutoLocalizeContext(
    val project: Project,
    val baseFile: PsiFile,
)
