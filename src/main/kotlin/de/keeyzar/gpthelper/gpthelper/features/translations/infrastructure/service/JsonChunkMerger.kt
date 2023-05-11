package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

class JsonChunkMerger(private val objectMapper: ObjectMapper) {
    private val prettyPrinter = DefaultPrettyPrinter().withObjectIndenter(DefaultIndenter().withLinefeed("\n"))
    fun mergeChunks(jsonChunks: List<String>): String {
        val mergedNode = objectMapper.createObjectNode()

        jsonChunks.forEach { jsonChunk ->
            //if not surrounded by {}, then append it
            val sanitizedChunk = sanitizeChunk(jsonChunk)
            val chunkNode = objectMapper.readTree(sanitizedChunk)
            chunkNode.fields().forEach { (key, value) ->
                mergedNode.set<ObjectNode>(key, value)
            }
        }

        //does not allow \r\n
        return objectMapper.writer(prettyPrinter)
            .writeValueAsString(mergedNode)
    }

    private fun sanitizeChunk(jsonChunk: String) = if (!jsonChunk.startsWith("{")) {
        "{$jsonChunk}"
    } else {
        jsonChunk
    }
}
