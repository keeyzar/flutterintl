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

    fun fromResponse(targetLanguage: Language, gptResponse: String, baseTranslation: Translation): Translation {
        val map: Map<*,*> = objectMapper.readValue(gptResponse, Map::class.java)
        val desiredKey = baseTranslation.entry.desiredKey
        val entry = SimpleTranslationEntry(
            desiredKey = desiredKey,
            desiredValue = map[desiredKey] as String,
            desiredDescription = ((map["@${desiredKey}"] as Map<*, *>)["description"] ?: "") as String
        )
        return Translation(targetLanguage, entry)
    }
}
