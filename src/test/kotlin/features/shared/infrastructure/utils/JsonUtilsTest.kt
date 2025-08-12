package de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class JsonUtilsTest {
    private val sut = JsonUtils(ObjectMapper())

    @Test
    fun `test reorderJson when keys are in the same order`() {
        val base = "{\"key1\":\"value1\",\"key2\":\"value2\"}"
        val scrambled = "{\"key1\":\"scrambledValue1\",\"key2\":\"scrambledValue2\"}"
        val expected = "{\"key1\":\"scrambledValue1\",\"key2\":\"scrambledValue2\"}"
        val result = sut.reorderJson(base, scrambled)
        assertEquals(expected, result)
    }

    @Test
    fun `test reorderJson when keys are in a different order`() {
        val base = "{\"key1\":\"value1\",\"key2\":\"value2\"}"
        val scrambled = "{\"key2\":\"scrambledValue2\",\"key1\":\"scrambledValue1\"}"
        val expected = "{\"key1\":\"scrambledValue1\",\"key2\":\"scrambledValue2\"}"
        val result = sut.reorderJson(base, scrambled)
        assertEquals(expected, result)
    }

    @Test
    fun `test reorderJson when some keys are missing in scrambled JSON`() {
        val base = "{\"key1\":\"value1\",\"key2\":\"value2\",\"key3\":\"value3\"}"
        val scrambled = "{\"key1\":\"scrambledValue1\",\"key3\":\"scrambledValue3\"}"
        val expected = "{\"key1\":\"scrambledValue1\",\"key3\":\"scrambledValue3\"}"
        val result = sut.reorderJson(base, scrambled)
        assertEquals(expected, result)
    }

    @Test
    fun `test reorderJson when base JSON is empty`() {
        val base = "{}"
        val scrambled = "{\"key1\":\"scrambledValue1\",\"key2\":\"scrambledValue2\"}"
        val expected = "{}"
        val result = sut.reorderJson(base, scrambled)
        assertEquals(expected, result)
    }

    @Test
    fun `test reorderJson when scrambled JSON is empty`() {
        val base = "{\"key1\":\"value1\",\"key2\":\"value2\"}"
        val scrambled = "{}"
        val expected = "{}"
        val result = sut.reorderJson(base, scrambled)
        assertEquals(expected, result)
    }
}
