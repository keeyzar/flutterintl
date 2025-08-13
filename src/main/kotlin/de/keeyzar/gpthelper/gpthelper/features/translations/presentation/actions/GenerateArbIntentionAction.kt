package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.actions

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import com.jetbrains.lang.dart.psi.DartFile
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.MultiKeyTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.NotNull
import java.util.UUID


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
            val initializer = getInitializer()
            initializer.lastStatementProviderForFlutterArbTranslation.lastStatement = element
            val taskId = UUID.randomUUID().toString()
            val translationContext = TranslationContext(taskId, "Translation Init", 0, null, 0)

            initializer.translationTaskBackgroundProgress.triggerInBlockingContext(project, {
                try {
                    val preprocess = initializer.translationPreprocessor.preprocess(translationContext) ?: return@triggerInBlockingContext
                    val (processedContext, userTranslationInput) = preprocess

                    val translationRequest = initializer.userTranslationInputParser.toUserTranslationRequest(processedContext.baseLanguage, userTranslationInput)

                    if (!userTranslationInput.translateNow) {
                        // Nur Dummy-Eintrag erzeugen, keine Übersetzung
                        initializer.ongoingTranslationHandler.onlyGenerateBaseEntry(translationRequest)
                        initializer.translationTriggeredHooks.translationTriggered(translationRequest.baseTranslation)
                        return@triggerInBlockingContext
                    }

                    val multiKeyContext = MultiKeyTranslationContext(
                        baseLanguage = processedContext.baseLanguage,
                        targetLanguages = userTranslationInput.languagesToTranslate.map { Language.fromISOLangString(it.key) },
                        translationEntries = listOf(translationRequest.baseTranslation.entry)
                    )

                    initializer.multiKeyTranslationProcessController.startTranslationProcess(multiKeyContext)
                } finally {
                    translationContext.finished = true
                }
            }, translationContext = translationContext)
        }
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun checkFile(file: PsiFile?): Boolean {
        return super.checkFile(file) && file is DartFile
    }
}
