package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.controller

import com.intellij.openapi.application.ApplicationManager
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.MultiKeyTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets.RetryFailedTranslationsDialog
import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.ReviewService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.*
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.*
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client.DispatcherConfiguration
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.joinAll
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
    private val dispatcherConfiguration: DispatcherConfiguration,
) {

    suspend fun startTranslationProcess(multiKeyTranslationContext: MultiKeyTranslationContext) {
        val verified = settingsService.verifySettingsAndInformUserIfInvalid()
        if (!verified) {
            return
        }

        val requests = multiKeyTranslationContext.translationEntries.map {
            UserTranslationRequest(multiKeyTranslationContext.targetLanguages, Translation(multiKeyTranslationContext.baseLanguage, it))
        }
        multiKeyTranslationContext.taskAmount = multiKeyTranslationContext.targetLanguages.size * requests.size
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

        // val taskAmount = multiKeyTranslationContext.targetLanguages.size * requests.size
        // multiKeyTranslationContext.taskAmount = taskAmount
        processRequestsWithRetry(requests, multiKeyTranslationContext)

        multiKeyTranslationContext.finished = true
        multiKeyTranslationContext.finishedTasks = multiKeyTranslationContext.taskAmount
        multiKeyTranslationContext.progressText = "Auto Localize: ${multiKeyTranslationContext.taskAmount}/${multiKeyTranslationContext.taskAmount}"
        translationProgressBus.pushPercentage(TranslationProgress(multiKeyTranslationContext.taskAmount, multiKeyTranslationContext.taskAmount, multiKeyTranslationContext.uuid))
        translationTriggeredHooks.translationTriggeredPostTranslation()

        reviewService.askUserForReviewIfItIsTime()
    }

    private suspend fun processRequestsWithRetry(requests: List<UserTranslationRequest>, multiKeyTranslationContext: MultiKeyTranslationContext) {
        if (requests.isEmpty()) {
            return
        }

        val failedRequests = processTranslationRequests(requests, multiKeyTranslationContext)

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
                    processRequestsWithRetry(it, multiKeyTranslationContext)
                }
            }
        }
    }

    private suspend fun processTranslationRequests(requests: List<UserTranslationRequest>, multiKeyTranslationContext: MultiKeyTranslationContext): List<UserTranslationRequest> {
        val taskCounter = AtomicInteger(0)
        val failedRequests = mutableListOf<UserTranslationRequest>()

        // Calculate max parallel requests based on dispatcher parallelism and target languages
        val totalParallelism = dispatcherConfiguration.getLevelOfParallelism()
        val targetLanguagesCount = multiKeyTranslationContext.targetLanguages.size
        val maxParallelRequests = if (targetLanguagesCount > 0) {
            (totalParallelism / targetLanguagesCount).coerceAtLeast(1)
        } else {
            totalParallelism
        }

        coroutineScope {
            // Process requests in chunks to limit parallelism
            requests.chunked(maxParallelRequests).forEach { requestChunk ->
                requestChunk.map { request ->
                    launch(dispatcherConfiguration.getDispatcher()) {
                        val failedRequest = ongoingTranslationHandler.translateAsynchronouslyWithoutPlaceholder(request, true, { false }) {
                            reportProgress(taskCounter, multiKeyTranslationContext)
                        }
                        if (failedRequest != null) {
                            synchronized(failedRequests) {
                                failedRequests.add(failedRequest)
                            }
                        }
                    }
                }.joinAll() // Wait for all jobs in this chunk to complete before starting the next chunk
            }
        }
        return failedRequests
    }

    private fun reportProgress(realTaskCounter: AtomicInteger, multiKeyTranslationContext: MultiKeyTranslationContext) {
        val taskAmount = multiKeyTranslationContext.taskAmount
        val taskAmountHandled = realTaskCounter.incrementAndGet()
        multiKeyTranslationContext.finishedTasks = taskAmountHandled
        multiKeyTranslationContext.progressText = "Auto Localize: $taskAmountHandled/$taskAmount"
        val translationProgress = TranslationProgress(taskAmount, taskAmountHandled, multiKeyTranslationContext.uuid)

        translationProgressBus.pushPercentage(translationProgress)
    }
}
