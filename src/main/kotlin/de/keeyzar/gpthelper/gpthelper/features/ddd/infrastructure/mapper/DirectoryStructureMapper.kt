package de.keeyzar.gpthelper.gpthelper.features.ddd.infrastructure.mapper

import de.keeyzar.gpthelper.gpthelper.features.ddd.domain.entity.DirectoryStructure
import de.keeyzar.gpthelper.gpthelper.features.ddd.infrastructure.model.DirectoryStructureModel
import org.mapstruct.Mapper
import org.mapstruct.Mapping


@Mapper
interface DirectoryStructureMapper {
    @Mapping(target = "copy", ignore = true)
    fun toModel(entity: DirectoryStructure): DirectoryStructureModel

    @Mapping(target = "copy", ignore = true)
    fun toEntity(model: DirectoryStructureModel): DirectoryStructure
}
