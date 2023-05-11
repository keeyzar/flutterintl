package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

import kotlinx.coroutines.flow.Flow

interface DDDTranslationRequestClient {
    suspend fun requestTranslationOfSingleEntry(clientTranslationRequest: ClientTranslationRequest, partialTranslationFinishedCallback: (PartialTranslationResponse) -> Unit)
}
