package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language

/**
 * Batch request for creating multiple complex ARB entries at once
 */
data class BatchClientTranslationRequest(
    val targetLanguages: List<Language>,
    val requests: List<ClientTranslationRequest>
)

