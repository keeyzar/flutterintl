package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.controller

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessL10nClient
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessRequest
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.GatherBestGuessContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.GuessAdaptionService
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.MultiKeyTranslationTaskSizeEstimator
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.WaitingIndicatorService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.*
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.*
import kotlinx.coroutines.coroutineScope
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * controls the translation process from start to finish
 */
class BestGuessProcessController(
    private val settingsService: VerifyTranslationSettingsService,
    private val gatherBestGuessContext: GatherBestGuessContext,
    private val bestGuessL10nClient: BestGuessL10nClient,
    private val translationProgressBus: TranslationProgressBus,
    private val translationErrorProcessHandler: TranslationErrorProcessHandler,
    private val guessAdaptionService: GuessAdaptionService,
    private val multiKeyTranslationTaskSizeEstimator: MultiKeyTranslationTaskSizeEstimator,
    private val ongoingTranslationHandler: OngoingTranslationHandler,
    private val translationTriggeredHooks: TranslationTriggeredHooks,
    private val waitingIndicatorService: WaitingIndicatorService,
    ) {
    /**
     * Listen via the [TranslationProgressBus], but beware you might need to unregister yourself
     */
    suspend fun startBestGuessProcess(translationContext: TranslationContext, processUUID: UUID) {
        return try {
            _startBestGuessProcess(processUUID)
        } catch (e: Throwable) {
            translationErrorProcessHandler.displayErrorToUser(e)
        } finally {
            translationContext.finished = true
        }
    }

    private suspend fun _startBestGuessProcess(processUUID: UUID) {
        //as always, verify the settings first
        val verified = settingsService.verifySettingsAndInformUserIfInvalid()
        if (!verified) {
            return
        }

        val fileBestGuessContext = gatherBestGuessContext.getFileBestGuessContext(processUUID) ?: return
        waitingIndicatorService.startWaiting(processUUID, "Getting Translation key guess", "This might take a while, please be patient")
        val simpleGuess = bestGuessL10nClient.simpleGuess(BestGuessRequest(fileBestGuessContext))
        waitingIndicatorService.stopWaiting()
        val multiKeyTranslationContext = guessAdaptionService.adaptBestGuess(processUUID, simpleGuess) ?: return

        //now, that we have the translation context, we can start triggering the translation for these keys.
        //the issue is, that we theoretically have e.g. 10 translations for 20 files, which are 200 requests,
        //this will take a while, because we can only make 3 requests at the same time (for e.g. chatGPT)
        //though the user can proceed in the foreground with another task. e.g. prepare the next request
        //we can optimize later on, e.g. by using a different model ... when each request takes 3 seconds,
        //we should have about 3 minutes processing time for the whole file, which is... okay?
        //lets see how that turns out
        val taskAmount = multiKeyTranslationTaskSizeEstimator.estimateTaskSize(multiKeyTranslationContext)
        val taskCounter = AtomicInteger(0)
        coroutineScope {
            //we need to placeholder translate all the different things first, because otherwise the user
            //can't go on with his stuff
            val baseLanguage = multiKeyTranslationContext.baseLanguage
            val targetLanguages = multiKeyTranslationContext.targetLanguages
            val requests = multiKeyTranslationContext.translationEntries
                .map { UserTranslationRequest(targetLanguages, Translation(baseLanguage, it)) }
            //we need to run this placeholder stuff, otherwise we can't proceed
            requests.forEach {
                if(baseLanguage != it.baseTranslation.lang) {
                    ongoingTranslationHandler.translateWithPlaceholder(it) //don't add it there yet :)
                }
            }

            //flutter gen or something like that, because otherwise the keys are not resolvable
            //todo, we do not need to know these different steps here, because the
            //actual implementation can make a hash of the file and check, whether there has been some changes
            //please revert it
            translationTriggeredHooks.translationTriggeredInit()
            //now modify the file, because everything should be in place to do it
            requests.forEach {
                translationTriggeredHooks.translationTriggeredPartial(it.baseTranslation)
            }
            //okay, this here will take quite a while...
            requests.forEach {
                ongoingTranslationHandler.translateAsynchronouslyWithoutPlaceholder(it) {
                    reportProgress(taskCounter, taskAmount)
                }
            }
        }
        return
    }

    private fun reportProgress(realTaskCounter: AtomicInteger, taskAmount: Int) {
        val translationProgress = TranslationProgress(realTaskCounter.incrementAndGet(), taskAmount, "dummy");
        translationProgressBus.pushPercentage(translationProgress)
    }
}
