package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.dto.GPTArbTranslationResponse

interface SingleTranslationRequestClient {
    suspend fun requestTranslation(key: String, value: String, description: String, targetLanguage: String): GPTArbTranslationResponse
    suspend fun requestTranslation(content: String, targetLanguage: String): GPTArbTranslationResponse
    suspend fun requestTranslation(content: String, baseLanguage: Language, targetLanguage: Language): GPTArbTranslationResponse
}
