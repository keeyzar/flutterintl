package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.controller

import com.intellij.openapi.application.ApplicationManager
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.MultiKeyTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets.RetryFailedTranslationsDialog
import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.ReviewService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.*
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

        val requests = multiKeyTranslationContext.translationEntries.map {
            UserTranslationRequest(multiKeyTranslationContext.targetLanguages, Translation(multiKeyTranslationContext.baseLanguage, it))
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

        processRequestsWithRetry(requests, multiKeyTranslationContext.targetLanguages.size)

        reviewService.askUserForReviewIfItIsTime()
    }

    private suspend fun processRequestsWithRetry(requests: List<UserTranslationRequest>, languagesCount: Int) {
        if (requests.isEmpty()) {
            return
        }

        val failedRequests = processTranslationRequests(requests, languagesCount)

        if (failedRequests.isNotEmpty()) {
            var requestsToRetry: List<UserTranslationRequest>? = null
            ApplicationManager.getApplication().invokeAndWait {
                val dialog = RetryFailedTranslationsDialog(failedRequests)
                if (dialog.showAndGet()) {
                    requestsToRetry = dialog.getRequestsToRetry()
                }
            }

            requestsToRetry?.let {
                if (it.isNotEmpty()) {
                    processRequestsWithRetry(it, languagesCount)
                }
            }
        }
    }

    private suspend fun processTranslationRequests(requests: List<UserTranslationRequest>, languagesCount: Int): List<UserTranslationRequest> {
        val taskAmount = languagesCount * requests.size
        val taskCounter = AtomicInteger(0)
        val failedRequests = mutableListOf<UserTranslationRequest>()

        coroutineScope {
            // 3. Trigger asynchronous translation for all target languages
            requests.forEach { request ->
                launch {
                    val failedRequest = ongoingTranslationHandler.translateAsynchronouslyWithoutPlaceholder(request, true, { false }) {
                        reportProgress(taskCounter, taskAmount)
                    }
                    if (failedRequest != null) {
                        synchronized(failedRequests) {
                            failedRequests.add(failedRequest)
                        }
                    }
                }
            }
        }
        return failedRequests
    }

    private fun reportProgress(realTaskCounter: AtomicInteger, taskAmount: Int) {
        val translationProgress = TranslationProgress(realTaskCounter.incrementAndGet(), taskAmount, "dummy");
        translationProgressBus.pushPercentage(translationProgress)
    }
}
