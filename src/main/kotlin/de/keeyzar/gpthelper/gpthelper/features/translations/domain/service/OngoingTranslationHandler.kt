package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.BatchClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.DDDTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.PartialTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.ReplacementOfTranslationFailedException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.mapper.TranslationRequestMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client.DispatcherConfiguration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class OngoingTranslationHandler(
    private val translationRequestClient: DDDTranslationRequestClient,
    private val mapper: TranslationRequestMapper,
    private val arbFileModificationService: ArbFileModificationService,
    private val dispatcherConfiguration: DispatcherConfiguration,
) {

    companion object {
        const val BATCH_SIZE = 10
    }

    /**
     * first, all the translations are translated with dummy placeholder, so that the user can proceed
     * but afterward we will replace the files under the hood.
     * There will be at most a single translation task running at a time, so that the files are not modified concurrently
     * will translate the given [userTranslationRequest] asynchronously, and will call the listener for each finished translation, either
     * as a dummy translation (true, Translation) or as a real translation (false, Translation)
     */
    suspend fun translateAsynchronously(
        userTranslationRequest: UserTranslationRequest,
        isCancelled: () -> Boolean,
        progressReport: () -> Unit
    ) {
        translateAsynchronouslyWithoutPlaceholder(userTranslationRequest, false, isCancelled, progressReport)
    }

    /**
     * @return null if successful, or the request if it failed
     */
    suspend fun translateAsynchronouslyWithoutPlaceholder(
        userTranslationRequest: UserTranslationRequest,
        shouldFixArb: Boolean,
        isCancelled: () -> Boolean,
        progressReport: () -> Unit
    ): UserTranslationRequest? {
        //what we actually want to do is to get the initial conversion of the request done by GPT
        //and then, when the conversion has been done, a simple translation, which does not need to be done by an expensive model, but that
        //is an optimization for later

        val baseLanguage = userTranslationRequest.baseTranslation.lang


        val clientRequest = mapper.toClientRequest(userTranslationRequest);
        val success = translateInBackground(clientRequest, shouldFixArb, isCancelled, progressReport) {
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
        return if (success) null else userTranslationRequest

    }

    /**
     * trigger only the placeholders (when you have multiple placeholders
     */
    fun translateWithPlaceholder(userTranslationRequest: UserTranslationRequest) {
        userTranslationRequest.targetLanguages.forEach {
            arbFileModificationService.addSimpleTranslationEntry(
                Translation(
                    it,
                    userTranslationRequest.baseTranslation.entry
                )
            )
        }
    }

    fun onlyGenerateBaseEntry(userTranslationRequest: UserTranslationRequest) {
        arbFileModificationService.addSimpleTranslationEntry(userTranslationRequest.baseTranslation)
    }

    /**
     * Batch version: translates multiple requests at once (up to BATCH_SIZE per API call)
     * Processes batches in parallel according to configured parallelism
     * @return list of failed requests (empty if all succeeded)
     */
    suspend fun translateBatchAsynchronouslyWithoutPlaceholder(
        requests: List<UserTranslationRequest>,
        shouldFixArb: Boolean,
        isCancelled: () -> Boolean,
        progressReport: () -> Unit
    ): List<UserTranslationRequest> = coroutineScope {
        if (requests.isEmpty() || isCancelled()) {
            return@coroutineScope emptyList()
        }

        val failedRequests = mutableListOf<UserTranslationRequest>()
        val batches = requests.chunked(BATCH_SIZE)
        val dispatcher = dispatcherConfiguration.getDispatcher()
        val parallelism = dispatcherConfiguration.getLevelOfParallelism()

        // Process batches in chunks according to configured parallelism
        batches.chunked(parallelism).forEach { batchChunk ->
            if (isCancelled()) {
                failedRequests.addAll(batchChunk.flatten())
                return@forEach
            }

            val deferredResults = batchChunk.map { batch ->
                async(dispatcher) {
                    if (isCancelled()) {
                        return@async Pair(batch, false)
                    }

                    try {
                        val clientRequests = batch.map { mapper.toClientRequest(it) }
                        val targetLanguages = batch.first().targetLanguages
                        val batchRequest = BatchClientTranslationRequest(targetLanguages, clientRequests)

                        val baseLanguage = batch.first().baseTranslation.lang

                        val success = translateBatchInBackground(
                            batchRequest,
                            shouldFixArb,
                            isCancelled,
                            progressReport
                        ) { translation ->
                            if (baseLanguage == translation.lang) {
                                arbFileModificationService.replaceSimpleTranslationEntry(translation)
                            } else {
                                try {
                                    arbFileModificationService.replaceSimpleTranslationEntry(translation)
                                } catch (e: ReplacementOfTranslationFailedException) {
                                    arbFileModificationService.addSimpleTranslationEntry(translation)
                                }
                            }
                            progressReport()
                        }

                        Pair(batch, success)
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        Pair(batch, false)
                    }
                }
            }

            val results = deferredResults.awaitAll()
            results.forEach { (batch, success) ->
                if (!success) {
                    failedRequests.addAll(batch)
                }
            }
        }

        return@coroutineScope failedRequests
    }

    private suspend fun translateBatchInBackground(
        batchRequest: BatchClientTranslationRequest,
        shouldFixArb: Boolean,
        isCancelled: () -> Boolean,
        progressReport: () -> Unit,
        translationListener: (Translation) -> Unit
    ): Boolean {
        if (shouldFixArb) {
            try {
                val batchResponse = translationRequestClient.createComplexArbEntryBatch(batchRequest, isCancelled, progressReport)

                // Save all base language entries
                batchResponse.responses.forEach { partialResponse ->
                    arbFileModificationService.replaceSimpleTranslationEntry(partialResponse.translation)
                    progressReport()
                }

                // Translate all entries to target languages
                translationRequestClient.translateValueOnlyBatch(batchRequest, batchResponse, isCancelled) { partialTranslation ->
                    println("got batch translation for ${partialTranslation.getTargetLanguage()}")
                    translationListener(partialTranslation.translation)
                }
                return true
            } catch (e: Throwable) {
                e.printStackTrace()
                return false
            }
        } else {
            // For non-fix mode, use batch translation only
            val batchResponse = de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.BatchPartialTranslationResponse(
                batchRequest.requests.map { PartialTranslationResponse(it.translation) }
            )

            translationRequestClient.translateValueOnlyBatch(batchRequest, batchResponse, isCancelled) { partialTranslation ->
                println("got batch translation for ${partialTranslation.getTargetLanguage()}")
                translationListener(partialTranslation.translation)
            }
            return true
        }
    }


    private suspend fun translateInBackground(
        clientRequest: ClientTranslationRequest,
        shouldFixArb: Boolean,
        isCancelled: () -> Boolean,
        progressReport: () -> Unit, // Add progressReport here
        translationListener: (Translation) -> Unit
    ): Boolean {
        //first translate the base language with placeholder, we create a complex arb entry, based on the information we have here
        //this is not required, when we translate only, though!
        if (shouldFixArb) {
            try {
                val createdArbEntry = translationRequestClient.createComplexArbEntry(clientRequest)
                arbFileModificationService.replaceSimpleTranslationEntry(createdArbEntry.translation)
                progressReport() // Report progress after creating the complex ARB entry
                translationRequestClient
                    .translateValueOnly(clientRequest, createdArbEntry, isCancelled) { partialTranslation ->
                        println("got translation for ${partialTranslation.getTargetLanguage()}")
                        translationListener(partialTranslation.translation)
                    }
                return true
            } catch (e: Throwable) {
                //TODO proper logging
                e.printStackTrace()
                return false
            }
        } else {
            translationRequestClient
                .translateValueOnly(
                    clientRequest,
                    PartialTranslationResponse(clientRequest.translation),
                    isCancelled
                ) { partialTranslation ->
                    println("got translation for ${partialTranslation.getTargetLanguage()}")
                    translationListener(partialTranslation.translation)
                }
            return true
        }
        //then translate the other languages
    }
}
