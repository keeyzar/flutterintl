package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

/**
 * Response containing multiple partial translations
 */
data class BatchPartialTranslationResponse(
    val responses: List<PartialTranslationResponse>
)

