package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.presentation.intention

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.json.JsonLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import de.keeyzar.gpthelper.gpthelper.features.flutterarb.presentation.handler.ArbFileType
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import org.jetbrains.annotations.Nls
import java.util.*


class MissingTranslationAction : PsiElementBaseIntentionAction(), IntentionAction {

    private var flutterArbTranslationInitializer: FlutterArbTranslationInitializer? = null

    @Nls
    override fun getText(): String {
        return "Translate Missing"
    }

    @Nls
    override fun getFamilyName(): String {
        return "Translation"
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        // Add your availability logic here //filename should not be untranslated_messages.txt
        return element.containingFile.fileType === ArbFileType.INSTANCE && element.containingFile.name == "untranslated_messages.txt"
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

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        getInitializer().lastStatementProviderForFlutterArbTranslation.lastStatement = element
        val translationProcessController = getInitializer().missingTranslationController
        val uuid = UUID.randomUUID().toString()

        val missingTranslationContext = MissingTranslationContext(
            uuid = uuid,
            reference = element,
            missingTranslations = emptyList()
        )

        getInitializer().translationTaskBackgroundProgress.triggerInBlockingContext(project, {
            translationProcessController.startMissingTranslationProcess(missingTranslationContext)
        }, translationContext = missingTranslationContext)
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun checkFile(file: PsiFile?): Boolean {
        return file?.language === JsonLanguage.INSTANCE
    }
}