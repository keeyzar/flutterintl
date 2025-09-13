package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations


class ArbSuggestionServiceTest {
    @Mock
    lateinit var arbFileContentProvider: ArbFileContentProvider
    lateinit var service: ArbSuggestionService

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        service = ArbSuggestionService(arbFileContentProvider)
    }

    @Test
    fun test_suggest_prefix_orders_by_similarity() {
        val json = """
            {
              "common_label_save": "Save",
              "common_label_sample": "Sample",
              "other": "Other"
            }
        """.trimIndent()

        // populate index explicitly
        `when`(arbFileContentProvider.getBaseLangContent())
            .thenReturn(json)
        service.refreshIndexIfNeeded()

        val results = service.suggest("sa", 10)
        assertTrue(results.isNotEmpty())
        // best match should be "common_label_save" because "save" is closer to "sa" than "sample"
        assertEquals("common_label_save", results[0].key)
        // results contain at least save and sample
        val keys = results.map { it.key }
        assertTrue(keys.contains("common_label_save"))
        assertTrue(keys.contains("common_label_sample"))
    }

    @Test
    fun test_suggest_trigram_match() {
        val json = """
            {
              "common_label_save": "Save",
              "other": "Other"
            }
        """.trimIndent()
        `when`(arbFileContentProvider.getBaseLangContent())
            .thenReturn(json)
        service.refreshIndexIfNeeded()

        val results = service.suggest("ave", 10)
        assertTrue(results.isNotEmpty())
        // trigram 'ave' should match 'save'
        assertEquals("common_label_save", results[0].key)
    }

    @Test
    fun test_suggest_empty_input_returns_empty() {
        val json = """
            {
              "common_label_save": "Save"
            }
        """.trimIndent()
        `when`(arbFileContentProvider.getBaseLangContent())
            .thenReturn(json)
        service.refreshIndexIfNeeded()

        val results = service.suggest("", 10)
        assertTrue(results.isEmpty())
    }
}