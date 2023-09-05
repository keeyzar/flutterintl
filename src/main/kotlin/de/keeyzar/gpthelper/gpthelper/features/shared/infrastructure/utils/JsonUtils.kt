package de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.ReplacementOfTranslationFailedException

class JsonUtils(private val objectMapper: ObjectMapper) {
    fun hasAnyEntry(jsonString: String): Boolean {
        return jsonString.contains(":")
    }

    fun reorderJson(base: String, scrambled: String): String {
        val objectMapper = ObjectMapper()
        val baseJsonNode = objectMapper.readTree(base) as ObjectNode
        val scrambledJsonNode = objectMapper.readTree(scrambled) as ObjectNode

        val orderedJsonNode = objectMapper.createObjectNode()

        baseJsonNode.fieldNames().forEachRemaining { key ->
            val node = scrambledJsonNode.get(key)
            if (node != null) {
                orderedJsonNode.set<JsonNode>(key, node)
            }
        }

        return objectMapper.writeValueAsString(orderedJsonNode)
    }

    fun replaceKeys(oldContent: String, simpleTranslationEntry: SimpleTranslationEntry): String {
        val newMap = objectMapper.readValue(oldContent, Map::class.java)
            .toMutableMap()
        entryToMap(simpleTranslationEntry).forEach { (key, value) ->
            if (!newMap.containsKey(key)) {
                throw ReplacementOfTranslationFailedException("Translation failed. Key not found in old translations $key")
            }
            newMap[key] = value
        }
        return objectMapper.writeValueAsString(newMap)
    }

    fun removeTrailingComma(jsonString: String): String {
        return jsonString.trim().removeSuffix(",")
    }

    fun removeSurroundingBrackets(jsonString: String): String {
        return jsonString.trim().removeSurrounding("{", "}")
    }

    private fun entryToMap(simpleTranslationEntry: SimpleTranslationEntry) = simpleTranslationEntry.toMap()
    fun entryToJsonString(simpleTranslationEntry: SimpleTranslationEntry): String {
        if (simpleTranslationEntry.desiredDescription.isBlank()) {
            return objectMapper.writeValueAsString(mapOf(simpleTranslationEntry.desiredKey to simpleTranslationEntry.desiredValue))
        }
        return objectMapper.writeValueAsString(simpleTranslationEntry.toMap())
    }

    fun entryToJsonStringWithoutSurroundingBrackets(simpleTranslationEntry: SimpleTranslationEntry) =
        removeSurroundingBrackets(entryToJsonString(simpleTranslationEntry))

    fun mapToJsonString(map: Map<String, *>): String = removeSurroundingBrackets(objectMapper.writeValueAsString(map))

    private fun SimpleTranslationEntry.toMap(): Map<String, *> {
        val map = mutableMapOf<String, Any>()
        map[this.desiredKey] = this.desiredValue
        val additionalContent: MutableMap<String, Any> = mutableMapOf(
            "description" to this.desiredDescription,
        )
        if (this.placeholder != null) {
            additionalContent["placeholders"] = this.placeholder
        }
        map["@" + this.desiredKey] = additionalContent
        return map;
    }
}
