package de.keeyzar.gpthelper.gpthelper.features.ddd.infrastructure.mapper

import de.keeyzar.gpthelper.gpthelper.features.ddd.domain.entity.DirectoryStructure
import de.keeyzar.gpthelper.gpthelper.features.ddd.infrastructure.model.DirectoryStructureModel
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.mapstruct.factory.Mappers

class DirectoryStructureMapperTest {
    val sut = Mappers.getMapper(DirectoryStructureMapper::class.java)
    @Test
    fun `toModel should map entity to model`() {
        val entity = DirectoryStructure(
            subDirectories = listOf("sub1", "sub2")
        )

        val model = sut.toModel(entity)

        assertThat(model.subDirectories).isEqualTo(entity.subDirectories)
    }


    @Test
    fun `toModel should map model to entity`() {
        val model = DirectoryStructureModel(
            subDirectories = listOf("sub1", "sub2")
        )

        val entity = sut.toEntity(model)

        assertThat(entity.subDirectories).isEqualTo(model.subDirectories)
    }
}
