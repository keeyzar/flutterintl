package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.dto.GPTArbTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.parser.GPTARBResponseParser


/**
 * will immediately provide a translation
 */
class ImmediateTranslationService(
    private val templateService: ARBTemplateService,
    private val responseParser: GPTARBResponseParser,
) {
    /**
     * gpt skips the own translation, therefore we provide multiple "responses"
     */
    fun requestTranslation(key: String, value: String, description: String): GPTArbTranslationResponse {
        return responseParser.parseResponse(templateService.fillTemplate(key, value, description));
    }
}
