package de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.controller

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.TranslationClient
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.factories.TranslationRequestFactory
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.FinishedFileTranslationHandler
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.GatherFileTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.PartialFileResponseHandler
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger

/**
 * controls the translation process from start to finish
 */
class FileTranslationProcessController(
    private val settingsService: VerifyTranslationSettingsService,
    private val gatherFileTranslationContext: GatherFileTranslationContext,
    private val translationClient: TranslationClient,
    private val partialFileResponseHandler: PartialFileResponseHandler,
    private val translationRequestFactory: TranslationRequestFactory,
    private val finishedFileTranslationHandler: FinishedFileTranslationHandler,
    private val translationProgressBus: TranslationProgressBus,
    private val translationErrorProcessHandler: TranslationErrorProcessHandler,
) {
    /**
     * Listen via the [TranslationProgressBus], but beware you might need to unregister yourself
     */
    suspend fun startTranslationProcess(translationContext: TranslationContext, uuid: UUID) {
        return try {
            _startTranslationProcess(uuid)
        } catch (e: Throwable) {
            translationErrorProcessHandler.displayErrorToUser(e)
        } finally {
            translationContext.finished = true
        }
    }

    private suspend fun _startTranslationProcess(uuid: UUID) {
        val verified = settingsService.verifySettingsAndInformUserIfInvalid()
        if (!verified) {
            return
        }

        val translationContext = gatherFileTranslationContext.gatherTranslationContext(uuid) ?: return

        val translatedCounter = AtomicInteger(0)
        coroutineScope {
            val request = async {
                //I need a translation service, that translates and reports progress
                val request = translationRequestFactory.createRequest(translationContext)
                translationClient.translate(request) { partialTranslationResponse, taskSize ->
                    partialFileResponseHandler.handlePartialFileResponse(translationContext, partialTranslationResponse)
                    reportProgress(translatedCounter, taskSize)
                }

            }
            request.await()
            finishedFileTranslationHandler.finishedTranslation(translationContext)
        }
        return
    }

    private fun reportProgress(realTaskCounter: AtomicInteger, taskAmount: Int) {
        val translationProgress = TranslationProgress(realTaskCounter.incrementAndGet(), taskAmount, "dummy")
        translationProgressBus.pushPercentage(translationProgress)
    }
}
