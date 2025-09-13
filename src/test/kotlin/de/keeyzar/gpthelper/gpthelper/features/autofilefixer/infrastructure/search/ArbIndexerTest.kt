package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArbIndexerTest {

    private val indexer = ArbIndexer()

    @Test
    fun `test_buildIndex should parse entries and ignore metadata starting with at`() {
        val json = """
            {
              "common_label_save": "Save",
              "greet": "Grüezi",
              "short": "Hi",
              "phrase": "Hello world",
              "@metadata": { "locale": "en" }
            }
        """.trimIndent()

        val index = indexer.buildIndex(json)

        // metadata should be ignored
        assertEquals(4, index.size)

        val keys = index.map { it.key }
        assertTrue(keys.contains("common_label_save"))
        assertTrue(keys.contains("greet"))
        assertTrue(keys.contains("short"))
        assertTrue(keys.contains("phrase"))
        assertFalse(keys.any { it.startsWith("@") })
    }

    @Test
    fun `test_normalizedValue should be lowercase and diacritics removed`() {
        val json = """{"greet":"Grüezi"}"""
        val index = indexer.buildIndex(json)
        val entry = index.find { it.key == "greet" }
        assertNotNull(entry)
        assertEquals("gruezi", entry!!.normalizedValue)
    }

    @Test
    fun `test_generateTrigrams should return single token for short strings`() {
        val json = """{"short":"Hi"}"""
        val index = indexer.buildIndex(json)
        val entry = index.find { it.key == "short" }
        assertNotNull(entry)
        assertEquals(setOf("hi"), entry!!.trigrams)
    }

    @Test
    fun `test_generateTrigrams should produce overlapping 3-char sequences for longer strings`() {
        val json = """{"phrase":"Hello world"}"""
        val index = indexer.buildIndex(json)
        val entry = index.find { it.key == "phrase" }
        assertNotNull(entry)
        val trigrams = entry!!.trigrams

        // cleaned string is "hello world" (length 11) so there should be 9 trigrams
        assertEquals(9, trigrams.size)
        // check some expected trigrams
        assertTrue(trigrams.contains("hel"))
        assertTrue(trigrams.contains("lo "))
        assertTrue(trigrams.contains("wor"))
        assertTrue(trigrams.contains("rld"))
    }

    @Test
    fun `test_save example should produce sav and ave trigrams`() {
        val json = """{"common_label_save":"Save"}"""
        val index = indexer.buildIndex(json)
        val entry = index.find { it.key == "common_label_save" }
        assertNotNull(entry)
        assertEquals("save", entry!!.normalizedValue)
        assertEquals(setOf("sav", "ave"), entry.trigrams)
    }
}