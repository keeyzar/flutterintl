package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.UserSettingsMapper

class PropertiesUserSettingsRepository(
    private val currentProjectProvider: CurrentProjectProvider,
    private val userSettingsMapper: UserSettingsMapper,
    private val userSettingsPersistentStateComponent: UserSettingsPersistentStateComponent,
) : UserSettingsRepository {

    override fun overrideSettings(callback: (oldUserSettings: UserSettings) -> UserSettings) {
        val newSettings = callback(getSettings())
        persistSettings(newSettings)
    }

    override fun saveSettings(userSettings: UserSettings) {
        persistSettings(userSettings)
    }

    private fun persistSettings(userSettings: UserSettings) {
        val sanitized = sanitize(userSettings);
        val model = userSettingsMapper.toModel(sanitized)
        userSettingsPersistentStateComponent.setNewState(model)
    }

    private fun sanitize(userSettings: UserSettings): UserSettings {
        val projectBasePath = currentProjectProvider.project.basePath ?: ""
        //sometimes, especially when coming from the frontend buttons, we have backslashes instead of slashes
        //therefore we ensure both types are found
        val projectBasePathBackslash = projectBasePath.replace("/", "\\")
        return userSettings.copy(
            arbDir = userSettings.arbDir?.removePrefix(projectBasePath)?.removePrefix(projectBasePathBackslash),
            intlConfigFile = userSettings.intlConfigFile
        )
    }

    /**
     * if there is no settings, we provide default user settings
     */
    override fun getSettings(): UserSettings {
        return userSettingsMapper.toEntity(userSettingsPersistentStateComponent.state)
    }

    /**
     * wipe the user settings
     */
    override fun wipeUserSettings() {
        userSettingsPersistentStateComponent.setNewState(UserSettingsPersistentStateComponent.UserSettingsModel())
    }
}
