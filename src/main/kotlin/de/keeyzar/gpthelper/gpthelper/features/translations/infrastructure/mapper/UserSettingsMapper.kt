package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.model.UserSettingsModel
import org.mapstruct.Mapper

@Mapper
interface UserSettingsMapper {
    fun toEntity(userSettingsModel: UserSettingsModel): UserSettings
    fun toModel(userSettings: UserSettings): UserSettingsModel
}
