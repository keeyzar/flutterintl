package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JsonChunkMergerTest {
    private val objectMapper = ObjectMapper()
    private val merger = JsonChunkMerger(objectMapper)

    @Test
    fun `mergeJsonChunks should merge list of JSON chunks into single JSON object`() {
        val chunk1 = """{"prop1":1,"prop2":2,"prop3":3}"""
        val chunk2 = """{"prop4":4,"prop5":5}"""
        val expectedMergedJson = """{"prop1":1,"prop2":2,"prop3":3,"prop4":4,"prop5":5}"""

        val jsonChunks = listOf(chunk1, chunk2)
        val actualMergedJson = merger.mergeChunks(jsonChunks).replace("\\s".toRegex(), "")

        assertThat(actualMergedJson).isEqualTo(expectedMergedJson)
    }

    @Test
    fun `mergeJsonChunks should merge list of JSON properties chunks into single JSON object`() {
        val chunk1 = """"prop1":1,"prop2":2,"prop3":3"""
        val chunk2 = """"prop4":4,"prop5":5"""
        val expectedMergedJson = """{"prop1":1,"prop2":2,"prop3":3,"prop4":4,"prop5":5}"""

        val jsonChunks = listOf(chunk1, chunk2)
        val actualMergedJson = merger.mergeChunks(jsonChunks).replace("\\s".toRegex(), "")

        assertThat(actualMergedJson).isEqualTo(expectedMergedJson)
    }

    @Test
    fun `mergeJsonChunks should return empty object if list of JSON chunks is empty`() {
        val expectedMergedJson = "{}"

        val jsonChunks = emptyList<String>()
        val actualMergedJson = merger.mergeChunks(jsonChunks).replace("\\s".toRegex(), "")

        assertThat(actualMergedJson).isEqualTo(expectedMergedJson)
    }

    @Test
    fun `mergeJsonChunks should merge list of JSON chunks with duplicate keys`() {
        val chunk1 = """{"prop1":1,"prop2":2,"prop3":3}"""
        val chunk2 = """{"prop3":4,"prop5":5}"""
        val expectedMergedJson = """{"prop1":1,"prop2":2,"prop3":4,"prop5":5}"""

        val jsonChunks = listOf(chunk1, chunk2)
        val actualMergedJson = merger.mergeChunks(jsonChunks).replace("\\s".toRegex(), "")

        assertThat(actualMergedJson).isEqualTo(expectedMergedJson)
    }
}
