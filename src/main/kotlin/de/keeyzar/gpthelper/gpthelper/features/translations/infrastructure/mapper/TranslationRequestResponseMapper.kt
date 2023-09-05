package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation

class TranslationRequestResponseMapper(private val objectMapper: ObjectMapper) {
    fun toGPTContent(translation: Translation): String {
        val simpleTranslationEntry = translation.entry;
        val map = mutableMapOf<String, Any>()
        map[simpleTranslationEntry.desiredKey] = simpleTranslationEntry.desiredValue
        map["@${simpleTranslationEntry.desiredKey}"] = mapOf("description" to simpleTranslationEntry.desiredDescription)
        return objectMapper.writeValueAsString(map)
    }
    fun toGPTContentAdvanced(translation: Translation): String {
        return """
            key: "${translation.entry.desiredKey}"
            value: "${translation.entry.desiredValue}"
            description: "${translation.entry.desiredDescription}"
        """.trimIndent()
    }

    fun toTranslationOnly(translation: Translation): String {
        return """
            "${translation.entry.desiredKey}": "${translation.entry.desiredValue}"
        """.trimIndent()
    }

    fun fromResponse(targetLanguage: Language, gptResponse: String, baseTranslation: Translation): Translation {
        val map: Map<*,*> = objectMapper.readValue(gptResponse, Map::class.java)
        val desiredKey = baseTranslation.entry.desiredKey
        val metadata = map.get("@${desiredKey}") as Map<*, *>
        val entry = SimpleTranslationEntry(
            id = null,
            desiredKey = desiredKey,
            desiredValue = map[desiredKey] as String,
            desiredDescription = metadata.getOrDefault("description", "") as String,
            placeholder = metadata.getOrDefault("placeholders", null) as Map<String, *>?
        )
        return Translation(targetLanguage, entry)
    }

    fun fromTranslationOnlyResponse(targetLanguage: Language, gptResponse: String, baseTranslation: Translation): Translation {
        val map: Map<*,*> = objectMapper.readValue(gptResponse, Map::class.java)
        val desiredKey = baseTranslation.entry.desiredKey
        val entry = SimpleTranslationEntry(
            id = null,
            desiredKey = desiredKey,
            desiredValue = map[desiredKey] as String,
            desiredDescription = "",
            placeholder = null
        )
        return Translation(targetLanguage, entry)
    }
}
