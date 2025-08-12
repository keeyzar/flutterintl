package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

interface DDDTranslationRequestClient {
    suspend fun requestTranslationOfSingleEntry(clientTranslationRequest: ClientTranslationRequest, partialTranslationFinishedCallback: (PartialTranslationResponse) -> Unit)
    suspend fun translateValueOnly(clientTranslationRequest: ClientTranslationRequest, partialTranslationResponse: PartialTranslationResponse, isCancelled: () -> Boolean, partialTranslationFinishedCallback: (PartialTranslationResponse) -> Unit)
    suspend fun createComplexArbEntry(clientTranslationRequest: ClientTranslationRequest) : PartialTranslationResponse
}
