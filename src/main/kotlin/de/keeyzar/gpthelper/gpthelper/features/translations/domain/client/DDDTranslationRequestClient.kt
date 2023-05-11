package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

fun interface DDDTranslationRequestClient {
    suspend fun requestTranslationOfSingleEntry(clientTranslationRequest: ClientTranslationRequest, partialTranslationFinishedCallback: (PartialTranslationResponse) -> Unit)
}
