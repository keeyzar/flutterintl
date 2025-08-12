package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessRequest
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.model.AutoLocalizeContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.util.*


class AutoLocalizeFile : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val initializer = FlutterArbTranslationInitializer()
        val uuid = UUID.randomUUID()
        val translationContext = TranslationContext(uuid.toString(), "Auto Localizing File", 0, null,0)
        val project = event.project!!
        initializer.contextProvider.putAutoLocalizeContext(uuid, AutoLocalizeContext(project, event.getData(PlatformDataKeys.PSI_FILE)!!))

        initializer.translationTaskBackgroundProgress.triggerInBlockingContext(project,
            {
                try {
                    val fileBestGuessContext = initializer.gatherBestGuessContext.getFileBestGuessContext(uuid) ?: return@triggerInBlockingContext
                    initializer.waitingIndicatorService.startWaiting(uuid, "Getting Translation key guess", "This might take a while, please be patient")
                    val simpleGuess = initializer.bestGuessL10nClient.simpleGuess(BestGuessRequest(fileBestGuessContext))
                    initializer.waitingIndicatorService.stopWaiting()
                    val multiKeyTranslationContext = initializer.guessAdaptionService.adaptBestGuess(uuid, simpleGuess) ?: return@triggerInBlockingContext

                    initializer.bestGuessProcessController.startTranslationProcess(multiKeyTranslationContext)
                } finally {
                    translationContext.finished = true
                }
            },
            translationContext = translationContext,
            onFinished = {
                initializer.psiElementIdReferenceProvider.deleteAllElements()
                initializer.contextProvider.removeAutoLocalizeContext(uuid)
            }
        )
    }
}
