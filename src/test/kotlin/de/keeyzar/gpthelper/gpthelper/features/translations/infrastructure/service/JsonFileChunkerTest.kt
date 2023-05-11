package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JsonFileChunkerTest {
    private val objectMapper = ObjectMapper()
    private val chunker = JsonFileChunker(objectMapper)

    @Test
    fun `chunkJson should split JSON object into chunks of specified size`() {
        val json = """{"prop1": 1, "prop2": 2, "prop3": 3, "prop4": 4, "prop5": 5}"""
        val expectedChunks = listOf(
            """{"prop1":1,"prop2":2,"prop3":3}""",
            """{"prop4":4,"prop5":5}"""
        )

        val actualChunks = chunker.chunkJson(json, 3L)

        assertThat(actualChunks).hasSameSizeAs(expectedChunks)
        assertThat(actualChunks).containsExactlyElementsOf(expectedChunks)
    }

    @Test
    fun `chunkJson should split JSON object into single chunk if size is larger than object`() {
        val json = """{"prop1": 1, "prop2": 2, "prop3": 3}"""
        val expectedChunks = listOf(
            """{"prop1":1,"prop2":2,"prop3":3}"""
        )

        val actualChunks = chunker.chunkJson(json, 5L)

        assertThat(actualChunks).hasSameSizeAs(expectedChunks)
        assertThat(actualChunks).containsExactlyElementsOf(expectedChunks)
    }

    @Test
    fun `chunkJson should return empty list if JSON object is empty`() {
        val json = "{}"

        val actualChunks = chunker.chunkJson(json, 5L)

        assertThat(actualChunks).isEmpty()
    }

    @Test
    fun `huge size 2` () {
        val json = createJsonWithPropertiesAndLevels(numProperties = 400, numLevels = 3)

        val actualChunks = chunker.chunkJson(json, 30L);

        assertThat(actualChunks).hasSize(14)
    }

    @Test
    fun `huge size` () {
        val json = createJsonWithPropertiesAndLevels(numProperties = 2000, numLevels = 1)

        val actualChunks = chunker.chunkJson(json, 50L);

        assertThat(actualChunks).hasSize(40)
    }

    private fun createJsonWithPropertiesAndLevels(numProperties: Int, numLevels: Int): String {
        val objectMapper = ObjectMapper()
        val rootNode = objectMapper.createObjectNode()

        repeat(numProperties) { index ->
            val propertyNode = createNestedPropertyNode(numLevels)
            rootNode.set<ObjectNode>("prop$index", propertyNode)
        }

        return objectMapper.writeValueAsString(rootNode)
    }

    private fun createNestedPropertyNode(numLevels: Int): JsonNode {
        val objectMapper = ObjectMapper()
        val propertyNode = objectMapper.createObjectNode()
        var currentNode: ObjectNode = propertyNode

        repeat(numLevels) { level ->
            val nestedNode = objectMapper.createObjectNode()
            currentNode.set<ObjectNode>("prop$level", nestedNode)
            currentNode = nestedNode
        }

        return propertyNode
    }
}
