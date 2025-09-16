package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import com.jetbrains.lang.dart.psi.DartFile
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull


@NonNls
class GenerateArbIntentionAction : PsiElementBaseIntentionAction(), IntentionAction {

    /**
     * If this action is applicable, returns the text to be shown in the list of intention actions available.
     */
    @NotNull
    override fun getText(): String {
        return "Auto localize (string)"
    }

    /**
     * Returns text for name of this family of intentions.
     * It is used to externalize "auto-show" state of intentions.
     * It is also the directory name for the descriptions.
     *
     * @return the intention family name.
     */
    @NotNull
    override fun getFamilyName(): String {
        return "TranslationIntention"
    }


    /**
     * Checks whether this intention is available at the caret offset in file - the caret must sit just before a "?"
     * character in a ternary statement. If this condition is met, this intention's entry is shown in the available
     * intentions list.
     *
     *
     * Note: this method must do its checks quickly and return.
     *
     * @param project a reference to the Project object being edited.
     * @param editor  a reference to the object editing the project source
     * @param element a reference to the PSI element currently under the caret
     * @return `true` if the caret is in a literal string element, so this functionality should be added to the
     * intention menu or `false` for all other types of caret positions
     */
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        return isDartString(element)
    }

    private fun getInitializer(project: Project): FlutterArbTranslationInitializer {
        return FlutterArbTranslationInitializer.create(project)
    }

    private fun isDartString(element: PsiElement): Boolean {
        return getInitializer(element.project).flutterPsiService.isDartString(element)
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        val initializer = getInitializer(project)
        val directory = element.containingFile.virtualFile ?: return

        initializer.orchestrator.orchestrate(project, directory, "Auto Localizing String")
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun checkFile(file: PsiFile?): Boolean {
        return super.checkFile(file) && file is DartFile
    }
}

