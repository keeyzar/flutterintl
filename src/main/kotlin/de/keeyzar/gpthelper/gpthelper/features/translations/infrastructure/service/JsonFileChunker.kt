package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper

class JsonFileChunker(
    private val objectMapper: ObjectMapper,
) {
    /**
     * TODO it would be nice, if we would recursively check whether a chunk is too big, i.e. > 400 chars, and if yes, split it in half
     */
    fun chunkJsonBasedOnTotalStringSize(json: String, chunkSize: Long): List<String> {
        // Read the JSON string into a JsonNode object
        val jsonNode = objectMapper.readTree(json)

        // Split the JsonNode into chunks
        val chunks = mutableListOf<Map<String, JsonNode>>()
        var currentChunk = mutableMapOf<String, JsonNode>()
        var count = 0L

        jsonNode.fields().forEach { (key, value) ->
            val tokens = key.length + value.toString().length // count tokens as keys + values lengths

            // If adding the next key/value pair would make the current chunk exceed the chunk size, add the current chunk to the list of chunks
            if (count + tokens > chunkSize && currentChunk.isNotEmpty()) {
                chunks.add(currentChunk.toMap())
                currentChunk = mutableMapOf()
                count = 0
            }

            currentChunk[key] = value
            count += tokens
        }

        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toMap())
        }

        // Filter out any empty chunks and write each chunk as a string
        return chunks
            .filter { it.isNotEmpty() }
            .map { objectMapper.writeValueAsString(it) }
    }
}

