package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class JsonFileChunker(
    private val objectMapper: ObjectMapper,
) {
    fun chunkJson(json: String, chunkSize: Long): List<String> {
        // Read the JSON string into a JsonNode object
        val jsonNode = objectMapper.readTree(json)

        // Split the JsonNode into chunks of 30 properties each
        val chunks = mutableListOf<Map<String, JsonNode>>()
        var currentChunk = mutableMapOf<String, JsonNode>()
        var count = 0

        jsonNode.fields().forEach { (key, value) ->
            currentChunk[key] = value
            count++

            if (count >= chunkSize) {
                chunks.add(currentChunk.toMap())
                currentChunk = mutableMapOf()
                count = 0
            }
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toMap())
        }

        // Write each chunk as a string
        return chunks.map { chunk ->
            objectMapper.writeValueAsString(chunk)
        }
    }
}

