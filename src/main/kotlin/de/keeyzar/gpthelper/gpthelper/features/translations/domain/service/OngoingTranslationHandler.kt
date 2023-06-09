package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.DDDTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationRequest
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
    suspend fun translateAsynchronously(userTranslationRequest: UserTranslationRequest, progressReport: () -> Unit) {
        translateWithPlaceholder(userTranslationRequest) {
            arbFileModificationService.addSimpleTranslationEntry(it)
        }
        return translateAsynchronouslyWithoutPlaceholder(userTranslationRequest, progressReport)
    }

    suspend fun translateAsynchronouslyWithoutPlaceholder(userTranslationRequest: UserTranslationRequest, progressReport: () -> Unit) {
        concurrentTranslationTasks.withPermit {
            val clientRequest = mapper.toClientRequest(userTranslationRequest);
            return translateInBackground(clientRequest) {
                arbFileModificationService.replaceSimpleTranslationEntry(it)
                progressReport()
            }
        }
    }

    /**
     * trigger only the placeholders (when you have multiple placeholders
     */
    fun translateWithPlaceholder(userTranslationRequest: UserTranslationRequest) {
        translateWithPlaceholder(userTranslationRequest) {
            arbFileModificationService.addSimpleTranslationEntry(it)
        }
    }

    private fun translateWithPlaceholder(userTranslationRequest: UserTranslationRequest, translationListener: (Translation) -> Unit) {
        userTranslationRequest.targetLanguages.forEach {
            translationListener(Translation(it, userTranslationRequest.baseTranslation.entry))
        }
    }

    private suspend fun translateInBackground(
        clientRequest: ClientTranslationRequest,
        translationListener: (Translation) -> Unit
    ) {
        return translationRequestClient
            .requestTranslationOfSingleEntry(clientRequest) { partialTranslation ->
                println("got translation for ${partialTranslation.getTargetLanguage()}")
                translationListener(partialTranslation.translation)
            }
    }
}
