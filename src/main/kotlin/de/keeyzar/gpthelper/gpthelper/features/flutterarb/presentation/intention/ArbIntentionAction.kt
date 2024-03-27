package de.keeyzar.gpthelper.gpthelper.features.flutterarb.presentation.intention

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.features.flutterarb.presentation.handler.ArbFileType
import org.jetbrains.annotations.Nls


class ArbIntentionAction : PsiElementBaseIntentionAction(), IntentionAction {
    @Nls
    override fun getText(): String {
        return "Change translation"
    }

    @Nls
    override fun getFamilyName(): String {
        return "Translation"
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        // Add your availability logic here
        return element.containingFile.fileType === ArbFileType.INSTANCE
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        //print the current json element
        println(element.text)
    }
}