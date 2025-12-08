package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation

class TranslationRequestResponseMapper(private val objectMapper: ObjectMapper) {
    fun toGPTContent(translation: Translation): String {
        val simpleTranslationEntry = translation.entry
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
        val simpleTranslationEntry = translation.entry
        val map = mutableMapOf<String, Any>()
        map[simpleTranslationEntry.desiredKey] = simpleTranslationEntry.desiredValue
        val metadataMap = mutableMapOf<String, Any>()
        metadataMap["description"] = simpleTranslationEntry.desiredDescription
        simpleTranslationEntry.placeholder?.let {
            metadataMap["placeholders"] = it
        }
        map["@${simpleTranslationEntry.desiredKey}"] = metadataMap
        return objectMapper.writeValueAsString(map)
    }

    fun fromResponse(targetLanguage: Language, gptResponse: String, baseTranslation: Translation): Translation {
        var modifiedResponse = gptResponse
        if(gptResponse.contains("```json")) {
            modifiedResponse = gptResponse.substringAfter("```json").substringBeforeLast("```")
        }
        val map: Map<*,*> = objectMapper.readValue(modifiedResponse, Map::class.java)
        val desiredKey = baseTranslation.entry.desiredKey
        val metadata = map.get("@${desiredKey}") as Map<*, *>
        val entry = SimpleTranslationEntry(
            id = null,
            desiredKey = desiredKey,
            desiredValue = map[desiredKey] as String,
            desiredDescription = metadata.getOrDefault("description", "") as String,
        )
        return Translation(targetLanguage, entry)
    }

    fun fromTranslationOnlyResponse(targetLanguage: Language, gptResponse: String, baseTranslation: Translation): Translation {
        var modifiedResponse = gptResponse
        if(gptResponse.contains("```json")) {
            modifiedResponse = gptResponse.substringAfter("```json").substringBeforeLast("```")
        }
        val map: Map<*,*> = objectMapper.readValue(modifiedResponse, Map::class.java)
        val desiredKey = baseTranslation.entry.desiredKey
        val metadata = map.get("@${desiredKey}") as? Map<*, *>
        try {
            val entry = SimpleTranslationEntry(
                id = null,
                desiredKey = desiredKey,
                desiredValue = map[desiredKey] as String,
                desiredDescription = metadata?.getOrDefault("description", "") as? String ?: "",
                placeholder = (metadata?.getOrDefault("placeholders", null) as? Map<*, *>) as? Map<String, *>?
            )
            return Translation(targetLanguage, entry)
        } catch (e: NullPointerException) {
            throw IllegalArgumentException("Response invalid. it looks like this: $gptResponse", e)
        }
    }

    // Batch methods for handling multiple translations at once
    fun toBatchGPTContentAdvanced(translations: List<Translation>): String {
        return translations.mapIndexed { index, translation ->
            """
            Entry ${index + 1}:
            key: "${translation.entry.desiredKey}"
            value: "${translation.entry.desiredValue}"
            description: "${translation.entry.desiredDescription}"
            """.trimIndent()
        }.joinToString("\n\n")
    }

    fun toBatchTranslationOnly(translations: List<Translation>): String {
        val batchMap = mutableMapOf<String, Any>()
        translations.forEach { translation ->
            val simpleTranslationEntry = translation.entry
            batchMap[simpleTranslationEntry.desiredKey] = simpleTranslationEntry.desiredValue
            val metadataMap = mutableMapOf<String, Any>()
            metadataMap["description"] = simpleTranslationEntry.desiredDescription
            simpleTranslationEntry.placeholder?.let {
                metadataMap["placeholders"] = it
            }
            batchMap["@${simpleTranslationEntry.desiredKey}"] = metadataMap
        }
        return objectMapper.writeValueAsString(batchMap)
    }

    fun fromBatchResponse(targetLanguage: Language, gptResponse: String, baseTranslations: List<Translation>): List<Translation> {
        var modifiedResponse = gptResponse
        if(gptResponse.contains("```json")) {
            modifiedResponse = gptResponse.substringAfter("```json").substringBeforeLast("```")
        }
        val map: Map<*,*> = objectMapper.readValue(modifiedResponse, Map::class.java)

        return baseTranslations.map { baseTranslation ->
            val desiredKey = baseTranslation.entry.desiredKey
            val metadata = map.get("@${desiredKey}") as? Map<*, *>
            val entry = SimpleTranslationEntry(
                id = null,
                desiredKey = desiredKey,
                desiredValue = map[desiredKey] as? String ?: baseTranslation.entry.desiredValue,
                desiredDescription = metadata?.getOrDefault("description", "") as? String ?: "",
                placeholder = (metadata?.getOrDefault("placeholders", null) as? Map<*, *>) as? Map<String, *>?
            )
            Translation(targetLanguage, entry)
        }
    }
}
