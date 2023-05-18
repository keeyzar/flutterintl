package de.keeyzar.gpthelper.gpthelper.features.review.infrastructure.mapper

import de.keeyzar.gpthelper.gpthelper.features.review.domain.entity.ReviewSettings
import de.keeyzar.gpthelper.gpthelper.features.review.infrastructure.model.ReviewSettingsModel
import org.mapstruct.Mapper

@Mapper
interface ReviewSettingsMapper {
    fun toModel(entity: ReviewSettings): ReviewSettingsModel
    fun toEntity(model: ReviewSettingsModel): ReviewSettings
}
