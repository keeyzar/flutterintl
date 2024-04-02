package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.DDDTranslationRequestClient
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
        val concurrentTranslationTasks = Semaphore(1)
    }

    /**
     * first, all the translations are translated with dummy placeholder, so that the user can proceed
     * but afterward we will replace the files under the hood.
     * There will be at most a single translation task running at a time, so that the files are not modified concurrently
     * will translate the given [userTranslationRequest] asynchronously, and will call the [translationListener] for each finished translation, either
     * as a dummy translation (true, Translation) or as a real translation (false, Translation)
     */
    suspend fun translateAsynchronously(userTranslationRequest: UserTranslationRequest,
                                        isCancelled: () -> Boolean,
                                        progressReport: () -> Unit) {
        arbFileModificationService.addSimpleTranslationEntry(userTranslationRequest.baseTranslation)
        return translateAsynchronouslyWithoutPlaceholder(userTranslationRequest, isCancelled, progressReport)
    }

    suspend fun translateAsynchronouslyWithoutPlaceholder(userTranslationRequest: UserTranslationRequest,
                                                          isCancelled: () -> Boolean,
                                                          progressReport: () -> Unit) {
        //what we actually want to do is to get the initial conversion of the request done by GPT
        //and then, when the conversion has been done, a simple translation, which does not need to be done by an expensive model, but that
        //is an optimization for later

        val baseLanguage = userTranslationRequest.baseTranslation.lang



        concurrentTranslationTasks.withPermit {
            val clientRequest = mapper.toClientRequest(userTranslationRequest);
            return translateInBackground(clientRequest, isCancelled) {
                if(baseLanguage == it.lang) {
                    arbFileModificationService.replaceSimpleTranslationEntry(it)
                } else {
                    try {
                        arbFileModificationService.replaceSimpleTranslationEntry(it)
                    } catch(e: ReplacementOfTranslationFailedException) {
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
        translateWithPlaceholder(userTranslationRequest) {
            arbFileModificationService.
            addSimpleTranslationEntry(it)
        }
    }

    private fun translateWithPlaceholder(userTranslationRequest: UserTranslationRequest, translationListener: (Translation) -> Unit) {
        userTranslationRequest.targetLanguages.forEach {
            translationListener(Translation(it, userTranslationRequest.baseTranslation.entry))
        }
    }

    private suspend fun translateInBackground(
        clientRequest: ClientTranslationRequest,
        isCancelled: () -> Boolean,
        translationListener: (Translation) -> Unit
    ) {
        //first translate the base language with placeholder
        val createdArbEntry = translationRequestClient.createARBEntry(clientRequest)
        arbFileModificationService.replaceSimpleTranslationEntry(createdArbEntry.translation)
        //then translate the other languages
        return translationRequestClient
            .translateValueOnly(clientRequest, createdArbEntry, isCancelled) { partialTranslation ->
                println("got translation for ${partialTranslation.getTargetLanguage()}")
                translationListener(partialTranslation.translation)
            }
    }
}
