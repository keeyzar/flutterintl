package de.keeyzar.gpthelper.gpthelper.features.changetranslation.presentation.intention

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.PsiElementBaseIntentionAction
import com.intellij.json.JsonLanguage
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import de.keeyzar.gpthelper.gpthelper.features.flutterarb.presentation.handler.ArbFileType
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.ChangeTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import org.jetbrains.annotations.Nls
import java.util.*


class ChangeTranslationAction : PsiElementBaseIntentionAction(), IntentionAction {

    private var flutterArbTranslationInitializer: FlutterArbTranslationInitializer? = null

    @Nls
    override fun getText(): String {
        return "Change translation"
    }

    @Nls
    override fun getFamilyName(): String {
        return "Translation"
    }

    override fun isAvailable(project: Project, editor: Editor, element: PsiElement): Boolean {
        // Add your availability logic here //filename should not be untranslated_messages.txt
        return element.containingFile.fileType === ArbFileType.INSTANCE && element.containingFile.name != "untranslated_messages.txt"
    }

    private fun getInitializer(project: Project): FlutterArbTranslationInitializer {
        return FlutterArbTranslationInitializer.create(project)
    }

    override fun invoke(project: Project, editor: Editor, element: PsiElement) {
        getInitializer(project).lastStatementProviderForFlutterArbTranslation.lastStatement = element
        val translationProcessController = getInitializer(project).translationProcessController
        val taskId = UUID.randomUUID().toString()
        val translationContext = TranslationContext(taskId, "Change Translation Init", 0, null, 0)
        val arbPsiUtils = getInitializer(project).arbPsiUtils
        arbPsiUtils.getCurrentJsonProperty(element)?.let { value ->
            translationContext.changeTranslationContext = ChangeTranslationContext(value.key, value.value, value.description)
        }

        getInitializer(project).translationTaskBackgroundProgress.triggerInBlockingContext(project, {
            translationProcessController.startTranslationProcess(translationContext)
        }, translationContext = translationContext)
    }


    /**
     * an arb file in general is a json file, that looks like this
     * {
     * ..
     * }
     * where on the first level all the keys are present
     */
    private fun isJsonKeyOnFirstLevel(element: PsiElement): Boolean {
        return false
    }

    override fun startInWriteAction(): Boolean {
        return false
    }

    override fun checkFile(file: PsiFile?): Boolean {
        return file?.language === JsonLanguage.INSTANCE
    }
}