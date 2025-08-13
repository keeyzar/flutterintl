package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.controller

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.MultiKeyTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.ReviewService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.*
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.*
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicInteger

/**
 * controls the translation process for multiple keys from start to finish
 */
class MultiKeyTranslationProcessController(
    private val settingsService: VerifyTranslationSettingsService,
    private val ongoingTranslationHandler: OngoingTranslationHandler,
    private val translationProgressBus: TranslationProgressBus,
    private val translationTriggeredHooks: TranslationTriggeredHooks,
    private val reviewService: ReviewService,
) {
    suspend fun startTranslationProcess(multiKeyTranslationContext: MultiKeyTranslationContext) {
        val verified = settingsService.verifySettingsAndInformUserIfInvalid()
        if (!verified) {
            return
        }

        processTranslationRequests(multiKeyTranslationContext)
        reviewService.askUserForReviewIfItIsTime()
    }

    private suspend fun processTranslationRequests(context: MultiKeyTranslationContext) {
        val taskAmount = context.targetLanguages.size * context.translationEntries.size
        val taskCounter = AtomicInteger(0)

        coroutineScope {
            val requests = context.translationEntries.map {
                UserTranslationRequest(context.targetLanguages, Translation(context.baseLanguage, it))
            }

            // 1. Add all base entries to the base ARB file
            requests.forEach {
                ongoingTranslationHandler.onlyGenerateBaseEntry(it)
            }

            // 2. Replace all string literals in the source code with the new keys
            translationTriggeredHooks.translationTriggeredInit()
            requests.forEach {
                translationTriggeredHooks.translationTriggeredPartial(it.baseTranslation)
            }
            translationTriggeredHooks.translationTriggeredPostTranslation()

            // 3. Trigger asynchronous translation for all target languages
            requests.forEach { request ->
                ongoingTranslationHandler.translateAsynchronouslyWithoutPlaceholder(request, true, { false }) {
                    reportProgress(taskCounter, taskAmount)
                }
            }
        }
    }

    private fun reportProgress(realTaskCounter: AtomicInteger, taskAmount: Int) {
        val translationProgress = TranslationProgress(realTaskCounter.incrementAndGet(), taskAmount, "dummy");
        translationProgressBus.pushPercentage(translationProgress)
    }
}
