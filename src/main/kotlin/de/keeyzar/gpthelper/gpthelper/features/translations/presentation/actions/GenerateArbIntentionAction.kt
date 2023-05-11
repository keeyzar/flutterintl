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

    private var flutterArbTranslationInitializer: FlutterArbTranslationInitializer? = null

    /**
     * If this action is applicable, returns the text to be shown in the list of intention actions available.
     */
    @NotNull
    override fun getText(): String {
        return "Generate translations"
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

    private fun getInitializer(): FlutterArbTranslationInitializer {
        return when (flutterArbTranslationInitializer) {
            null -> {
                flutterArbTranslationInitializer = FlutterArbTranslationInitializer()
                flutterArbTranslationInitializer!!
            }

            else -> flutterArbTranslationInitializer!!
        }
    }

    private fun isDartString(element: PsiElement): Boolean {
        return getInitializer().flutterPsiService.isDartString(element)
    }

    @Throws(IncorrectOperationException::class)
    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
        if (isDartString(element)) {
            //one issue we have here is, that we might invoke it a second time, while the old translation is running,
            //therefore I need to make sure, that I have some kind of "identifier" for the last translation process?
            //or can I somehow pass it to the translation context, but the issue is, this is infrastructure / presentation layer
            //and the domain does not know about this kind of stuff (i.e. PsiElement)
            getInitializer().lastStatementProviderForFlutterArbTranslation.lastStatement = element
            val translationProcessController = getInitializer().translationProcessController

            getInitializer().translationTaskBackgroundProgress.triggerInBlockingContext(project, {
                translationProcessController.startTranslationProcess()
            })
        }
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun checkFile(file: PsiFile?): Boolean {
        return super.checkFile(file) && file is DartFile
    }
}
