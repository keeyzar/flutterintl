package de.keeyzar.gpthelper.gpthelper.features.l10n.presentation.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.JsonUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class JsonUtilsTest {
    val sut = JsonUtils(ObjectMapper())

    @Test
    fun `should replace the string with duplicated commas`() {
        val someString = """
            {}
        """.trimIndent()

        val value = sut.removeDuplicatedCommas(someString)

        assertThat(value).isEqualTo("{}")
    }
    @Test
    fun `should replace the string with duplicated commas 2`() {
        val someString = """
            {"key": "value",,}
        """.trimIndent()

        val value = sut.removeDuplicatedCommas(someString)

        assertThat(value).isEqualTo("""{"key": "value",}""")
    }
    @Test
    fun `should replace the string with duplicated commas 3`() {
        val someString = """
            {"key": "value",
            ,
            "key2": "value"
            }
        """.trimIndent()

        val value = sut.removeDuplicatedCommas(someString)
            .replace("\\s".toRegex(), "")

        assertThat(value).isEqualTo("""{"key":"value","key2":"value"}""")
    }
    @Test
    fun `should replace the string with duplicated commas 4`() {
        val someString = """
            {"key": "value",
            ,
            "key2": "value",,
            "key3": "value"
            }
        """.trimIndent()

        val value = sut.removeDuplicatedCommas(someString)
            .replace("\\s".toRegex(), "")

        assertThat(value).isEqualTo("""{"key":"value","key2":"value","key3":"value"}""")
    }

    @Test
    fun `should replace the string with duplicated commas 5`() {
        val someString = """
            {"key": "value",
            ,
            "key2": "value",,
            "key3": "va,,,lue"
            }
        """.trimIndent()

        val value = sut.removeDuplicatedCommas(someString)
            .replace("\\s".toRegex(), "")

        assertThat(value).isEqualTo("""{"key":"value","key2":"value","key3":"va,,,lue"}""")
    }
}
