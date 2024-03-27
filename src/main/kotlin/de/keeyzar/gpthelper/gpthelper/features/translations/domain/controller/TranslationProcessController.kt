package de.keeyzar.gpthelper.gpthelper.features.translations.domain.controller

import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.ReviewService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.TaskAmountCalculator
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.UserTranslationInputParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.*
import kotlinx.coroutines.*
import java.util.concurrent.LinkedBlockingQueue

/**
 * controls the translation process from start to finish
 */
class TranslationProcessController(
    private val translationPreprocessor: TranslationPreprocessor,
    private val userTranslationInputParser: UserTranslationInputParser,
    private val ongoingTranslationHandler: OngoingTranslationHandler,
    private val translationProgressBus: TranslationProgressBus,
    private val translationErrorProcessHandler: TranslationErrorProcessHandler,
    private val taskAmountCalculator: TaskAmountCalculator,
    private val translationTriggeredHooks: TranslationTriggeredHooks,
    private val reviewService: ReviewService,
) {

    companion object {
        var QUEUE = LinkedBlockingQueue<TranslationContext>()
        var initialized = false
    }

    /**
     * Listen via the [TranslationProgressBus], but beware you might need to unregister yourself
     */
    suspend fun startTranslationProcess(translationContext: TranslationContext) {
        return try {
            startTranslationProcessInternal(translationContext)
            reviewService.askUserForReviewIfItIsTime()
        } catch (e: Throwable) {
            translationContext.finished = true
            translationErrorProcessHandler.displayErrorToUser(e)
        }
    }

    /**
     *
     */
    private fun startQueueIfNotRunning() {
        //TODO this is not thread safe
        if (initialized) {
            return
        }
        initialized = true

        val queue = QUEUE

        // Create a background thread to process the queue
        val backgroundThread = Thread {
            while (true) {
                val item = queue.take() // Blocks until an item is available
                runBlocking {
                    //we run in blocking context, because it's not required to run in parallel. it's fine to run sequentially,
                    //but we might do so in the future
                    try {
                        translationTaskHandler(item)
                    } catch (e: Throwable) {
                        item.finished = true
                        reportProgress(item)
                        translationErrorProcessHandler.displayErrorToUser(e)
                    }
                }
            }
        }
        backgroundThread.start()
    }

    private suspend fun translationTaskHandler(translationContext: TranslationContext) {
        println(translationContext.translationRequest!!.baseTranslation.entry.desiredValue)
        ongoingTranslationHandler.translateAsynchronouslyWithoutPlaceholder(translationContext.translationRequest!!) {
            translationContext.taskAmountHandled++
            reportProgress(translationContext)
        }
    }

    private suspend fun startTranslationProcessInternal(translationContextNew: TranslationContext) {
        val preprocess = translationPreprocessor.preprocess()

        val (translationContext, userTranslationInput) = if (preprocess != null) {
            preprocess
        } else {
            translationContextNew.finished = true
            reportProgress(translationContextNew)
            return
        }

        val translationRequest = userTranslationInputParser.toUserTranslationRequest(translationContext.baseLanguage, userTranslationInput)
        //this is correct, we have all the stuff now, that we need, we can put that into a queue
        val taskAmount = taskAmountCalculator.calculate(translationContext)
        val newTranslationContext = modifyTranslationContext(translationContextNew, translationRequest, taskAmount)

        ongoingTranslationHandler.translateWithPlaceholder(translationRequest) //dummy  translations
        translationTriggeredHooks.translationTriggered(translationRequest.baseTranslation)

        //but we need to prepare all the stuff first
        withContext(Dispatchers.IO) {
            QUEUE.put(newTranslationContext)
        }
        startQueueIfNotRunning()

        reportProgress(translationContextNew) //initial progress reporting
    }

    private fun modifyTranslationContext(
        translationContext: TranslationContext,
        translationRequest: UserTranslationRequest,
        taskAmount: Int,
    ): TranslationContext {
        translationContext.translationRequest = translationRequest
        translationContext.taskAmount = taskAmount
        translationContext.taskAmountHandled = 0
        translationContext.finished = false
        val desiredKey = translationRequest.baseTranslation.entry.desiredKey
        val desiredKeyMaxTenChars = desiredKey.substring(0, desiredKey.length.coerceAtMost(50))
        val readable = toReadable(desiredKeyMaxTenChars)
        translationContext.progressText = "Translating for: '$readable'"
        return translationContext
    }

    /**
     * converts e.g. helloDarkness to Hello Darkness and hello_darkness to Hello Darkness
     */
    private fun toReadable(key: String): String {
        @Suppress("NAME_SHADOWING")
        var key = key.replace("_", " ")
        key = key.replace(Regex("([a-z])([A-Z])")) {
            it.groupValues[1] + " " + it.groupValues[2]
        }
        return key
    }

    private fun reportProgress(translationContext: TranslationContext) {
        val taskAmount = translationContext.taskAmount
        val taskAmountHandled = translationContext.taskAmountHandled
        val translationProgress = TranslationProgress(taskAmount, taskAmountHandled, translationContext.id)
        if (taskAmountHandled+1 == taskAmount) {
            translationContext.finished = true
        }
        translationProgressBus.pushPercentage(translationProgress)
    }
}
