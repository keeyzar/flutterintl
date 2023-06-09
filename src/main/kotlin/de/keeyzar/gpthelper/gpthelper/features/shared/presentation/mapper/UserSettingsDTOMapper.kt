package de.keeyzar.gpthelper.gpthelper.features.shared.presentation.mapper

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.dto.UserSettingsDTO
import org.mapstruct.Mapper

@Mapper
interface UserSettingsDTOMapper {
    fun toModel(userSettingsDTO: UserSettingsDTO): UserSettings
    fun toDTO(userSettings: UserSettings): UserSettingsDTO
}
