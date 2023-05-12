package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.UserSettingsPersistentStateComponent.IdeaUserSettingsModel
import org.mapstruct.Mapper
import org.mapstruct.Mapping

@Mapper
interface NewUserSettingsMapper {
    @Mapping(target = "openAIKey", ignore = true)
    fun toEntity(ideaUserSettingsModel: IdeaUserSettingsModel): UserSettings
    fun toModel(userSettings: UserSettings): IdeaUserSettingsModel
}
