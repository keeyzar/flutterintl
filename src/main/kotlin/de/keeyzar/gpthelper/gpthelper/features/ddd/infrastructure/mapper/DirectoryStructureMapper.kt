package de.keeyzar.gpthelper.gpthelper.features.ddd.infrastructure.mapper

import de.keeyzar.gpthelper.gpthelper.features.ddd.domain.entity.DirectoryStructure
import de.keeyzar.gpthelper.gpthelper.features.ddd.infrastructure.model.DirectoryStructureModel
import org.mapstruct.Mapper
import org.mapstruct.factory.Mappers


@Mapper
interface DirectoryStructureMapper {
    fun toModel(entity: DirectoryStructure): DirectoryStructureModel
    fun toEntity(model: DirectoryStructureModel): DirectoryStructure
}
