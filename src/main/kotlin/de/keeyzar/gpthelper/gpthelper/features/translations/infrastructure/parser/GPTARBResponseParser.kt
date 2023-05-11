package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.parser

import com.fasterxml.jackson.databind.ObjectMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.dto.GPTArbTranslationResponse

class GPTARBResponseParser(private val jacksonMapper: ObjectMapper) {

    fun parseResponse(response: String): GPTArbTranslationResponse {
        val readValue = jacksonMapper.readValue(response, Map::class.java) as Map<String, Any>
        return GPTArbTranslationResponse(content = readValue)
    }
}

