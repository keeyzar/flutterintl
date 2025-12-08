package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.FileTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.PartialFileTranslationResponse

interface DDDTranslationRequestClient {
    suspend fun translateValueOnly(clientTranslationRequest: ClientTranslationRequest, partialTranslationResponse: PartialTranslationResponse, isCancelled: () -> Boolean, partialTranslationFinishedCallback: (PartialTranslationResponse) -> Unit)
    suspend fun createComplexArbEntry(clientTranslationRequest: ClientTranslationRequest) : PartialTranslationResponse

    // Batch methods for improved performance
    suspend fun createComplexArbEntryBatch(batchRequest: BatchClientTranslationRequest, isCancelled: () -> Boolean, progressReport: () -> Unit): BatchPartialTranslationResponse
    suspend fun translateValueOnlyBatch(batchRequest: BatchClientTranslationRequest, batchResponse: BatchPartialTranslationResponse, isCancelled: () -> Boolean, partialTranslationFinishedCallback: (PartialTranslationResponse) -> Unit)

    suspend fun translate(
        fileTranslationRequest: FileTranslationRequest,
        partialTranslationFinishedCallback: (PartialFileTranslationResponse, taskSize: Int) -> Unit
    )
}
