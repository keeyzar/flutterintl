package de.keeyzar.gpthelper.gpthelper.features.translations.domain.controller

import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.ReviewService
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.ThreadingService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.TaskAmountCalculator
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.UserTranslationInputParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.*

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
    private val threadingService: ThreadingService<TranslationContext>
) {
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

    private suspend fun translationTaskHandler(translationContext: TranslationContext) {
        println(translationContext.translationRequest!!.baseTranslation.entry.desiredValue)
        ongoingTranslationHandler.translateAsynchronouslyWithoutPlaceholder(translationContext.translationRequest!!,true, translationContext::isCancelled) {
            translationContext.taskAmountHandled++
            reportProgress(translationContext)
        }
    }

    private suspend fun startTranslationProcessInternal(translationContextNew: TranslationContext) {
        val preprocess = translationPreprocessor.preprocess(translationContextNew)

        val (translationContext, userTranslationInput) = if (preprocess != null) {
            preprocess
        } else {
            translationContextNew.finished = true
            reportProgress(translationContextNew)
            return
        }

        val translationRequest = userTranslationInputParser.toUserTranslationRequest(translationContext.baseLanguage, userTranslationInput)
        val taskAmount = taskAmountCalculator.calculate(translationContext)
        val newTranslationContext = modifyTranslationContext(translationContextNew, translationRequest, taskAmount)

        if (!userTranslationInput.translateNow) {
            // Nur Dummy-Eintrag erzeugen, keine Übersetzung
            ongoingTranslationHandler.onlyGenerateBaseEntry(translationRequest)
            translationTriggeredHooks.translationTriggered(translationRequest.baseTranslation)
            reportProgress(translationContextNew)
            return
        }

        if (translationContextNew.changeTranslationContext == null) {
            ongoingTranslationHandler.translateWithPlaceholder(translationRequest)
            translationTriggeredHooks.translationTriggered(translationRequest.baseTranslation)
        }

        threadingService.putIntoQueue(newTranslationContext)

        threadingService.startQueueIfNotRunning {
            try {
                translationTaskHandler(it)
            } catch (e: Throwable) {
                it.finished = true
                reportProgress(it)
                translationErrorProcessHandler.displayErrorToUser(e)
            }
        }

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
        val translationProgress = TranslationProgress(taskAmount, taskAmountHandled, translationContext.uuid)
        if (taskAmountHandled + 1 == taskAmount) {
            translationContext.finished = true
        }
        translationProgressBus.pushPercentage(translationProgress)
    }
}
