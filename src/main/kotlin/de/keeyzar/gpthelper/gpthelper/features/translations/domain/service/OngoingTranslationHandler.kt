package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.DDDTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.PartialTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.ReplacementOfTranslationFailedException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.mapper.TranslationRequestMapper
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class OngoingTranslationHandler(
    private val translationRequestClient: DDDTranslationRequestClient,
    private val mapper: TranslationRequestMapper,
    private val arbFileModificationService: ArbFileModificationService,
) {

    companion object {
        private val concurrentTranslationTasks = Semaphore(1)
    }

    /**
     * first, all the translations are translated with dummy placeholder, so that the user can proceed
     * but afterward we will replace the files under the hood.
     * There will be at most a single translation task running at a time, so that the files are not modified concurrently
     * will translate the given [userTranslationRequest] asynchronously, and will call the [translationListener] for each finished translation, either
     * as a dummy translation (true, Translation) or as a real translation (false, Translation)
     */
    suspend fun translateAsynchronously(
        userTranslationRequest: UserTranslationRequest,
        isCancelled: () -> Boolean,
        progressReport: () -> Unit
    ) {
        return translateAsynchronouslyWithoutPlaceholder(userTranslationRequest, false, isCancelled, progressReport)
    }

    suspend fun translateAsynchronouslyWithoutPlaceholder(
        userTranslationRequest: UserTranslationRequest,
        shouldFixArb: Boolean,
        isCancelled: () -> Boolean,
        progressReport: () -> Unit
    ) {
        //what we actually want to do is to get the initial conversion of the request done by GPT
        //and then, when the conversion has been done, a simple translation, which does not need to be done by an expensive model, but that
        //is an optimization for later

        val baseLanguage = userTranslationRequest.baseTranslation.lang



        concurrentTranslationTasks.withPermit {
            val clientRequest = mapper.toClientRequest(userTranslationRequest);
            return translateInBackground(clientRequest, shouldFixArb, isCancelled) {
                if (baseLanguage == it.lang) {
                    arbFileModificationService.replaceSimpleTranslationEntry(it)
                } else {
                    try {
                        arbFileModificationService.replaceSimpleTranslationEntry(it)
                    } catch (e: ReplacementOfTranslationFailedException) {
                        arbFileModificationService.addSimpleTranslationEntry(it)
                    }
                }
                progressReport()
            }
        }
    }

    /**
     * trigger only the placeholders (when you have multiple placeholders
     */
    fun translateWithPlaceholder(userTranslationRequest: UserTranslationRequest) {
        userTranslationRequest.targetLanguages.forEach {
            arbFileModificationService.addSimpleTranslationEntry(Translation(it, userTranslationRequest.baseTranslation.entry))
        }
    }

    fun onlyGenerateBaseEntry(userTranslationRequest: UserTranslationRequest) {
        arbFileModificationService.addSimpleTranslationEntry(userTranslationRequest.baseTranslation)
    }


    private suspend fun translateInBackground(
        clientRequest: ClientTranslationRequest,
        shouldFixArb: Boolean,
        isCancelled: () -> Boolean,
        translationListener: (Translation) -> Unit
    ) {
        //first translate the base language with placeholder, we create a complex arb entry, based on the information we have here
        //this is not required, when we translate only, though!
        if (shouldFixArb) {
            val createdArbEntry = translationRequestClient.createComplexArbEntry(clientRequest)
            arbFileModificationService.replaceSimpleTranslationEntry(createdArbEntry.translation)
            return translationRequestClient
                .translateValueOnly(clientRequest, createdArbEntry, isCancelled) { partialTranslation ->
                    println("got translation for ${partialTranslation.getTargetLanguage()}")
                    translationListener(partialTranslation.translation)
                }

        } else {
            return translationRequestClient
                .translateValueOnly(
                    clientRequest,
                    PartialTranslationResponse(clientRequest.translation),
                    isCancelled
                ) { partialTranslation ->
                    println("got translation for ${partialTranslation.getTargetLanguage()}")
                    translationListener(partialTranslation.translation)
                }

        }
        //then translate the other languages
    }
}
