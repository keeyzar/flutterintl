package de.keeyzar.gpthelper.gpthelper.features.translations.domain.controller

import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.ReviewService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.TaskAmountCalculator
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.UserTranslationInputParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.atomic.AtomicInteger

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
    /**
     * Listen via the [TranslationProgressBus], but beware you might need to unregister yourself
     */
    suspend fun startTranslationProcess() {
        return try {
            _startTranslationProcess()
            reviewService.askUserForReviewIfItIsTime()
        } catch (e: Throwable) {
            translationErrorProcessHandler.displayErrorToUser(e)
        }
    }

    private suspend fun _startTranslationProcess() {
        val (translationContext, userTranslationInput) = translationPreprocessor.preprocess() ?: return

        val translationRequest = userTranslationInputParser.toUserTranslationRequest(translationContext.baseLanguage, userTranslationInput)

        val taskAmount = taskAmountCalculator.calculate(translationContext);

        if (taskAmount == 0) {
            println("Nothing to translate")
            return
        }

        val translatedCounter = AtomicInteger(0)
        coroutineScope {
            val request = async {
                //I need a translation service, that translates and reports progress
                ongoingTranslationHandler.translateAsynchronously(translationRequest) {
                    reportProgress(translatedCounter, taskAmount)
                }
            }
            translationTriggeredHooks.translationTriggered(translationRequest.baseTranslation)
            request.await()
        }
        //we might need to revert this, if something happened in the overall process
        return
    }

    private fun reportProgress(realTaskCounter: AtomicInteger, taskAmount: Int) {
        val translationProgress = TranslationProgress(realTaskCounter.incrementAndGet(), taskAmount)
        translationProgressBus.pushPercentage(translationProgress)
    }
}
