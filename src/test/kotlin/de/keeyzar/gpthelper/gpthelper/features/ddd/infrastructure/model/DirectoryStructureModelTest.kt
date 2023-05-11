package de.keeyzar.gpthelper.gpthelper.features.ddd.infrastructure.model

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.ObjectMapperProvider
import org.assertj.core.api.Assertions
import org.junit.Test

class DirectoryStructureModelTest {
    private val sut = ObjectMapperProvider().provideObjectMapper(null)
    @Test
    fun `should create a valid json from model` () {
        val model = DirectoryStructureModel(
            subDirectories = listOf("sub1", "sub2")
        );
        val expected = """
            {
              "subDirectories" : [ "sub1", "sub2" ]
            }
        """.trimIndent()
            //remove all whitespace
            .replace("\\s".toRegex(), "")

        val serialized = sut.writeValueAsString(model)

        Assertions.assertThat(serialized).isEqualTo(expected)
    }

    @Test
    fun `should read a valid json to model` () {
        val json = """
            {
              "subDirectories" : [ "sub1", "sub2" ]
            }
        """.trimIndent()
            //remove all whitespace
            .replace("\\s".toRegex(), "")

        val expected = DirectoryStructureModel(
            subDirectories = listOf("sub1", "sub2")
        );

        val deserialized = sut.readValue(json, DirectoryStructureModel::class.java)

        Assertions.assertThat(deserialized).isEqualTo(expected)
    }

    @Test
    fun `should write and read arbitrarily complex directory structure` () {
        val model = DirectoryStructureModel(
            subDirectories = listOf("sub1", "sub2", "sub3", "sub1/sub4", "sub1/sub5", "sub1/sub6", "/sub1/sub5/sub7", "sub8", "sub9", "sub10")
        );

        val serialized = sut.writeValueAsString(model)
        val deserialized = sut.readValue(serialized, DirectoryStructureModel::class.java)

        Assertions.assertThat(deserialized).isEqualTo(model);
    }
}
