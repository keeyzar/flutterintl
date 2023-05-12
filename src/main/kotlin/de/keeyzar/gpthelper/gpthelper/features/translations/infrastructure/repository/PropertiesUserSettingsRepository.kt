package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationCredentialsServiceRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.NewUserSettingsMapper

class PropertiesUserSettingsRepository(
    private val currentProjectProvider: CurrentProjectProvider,
    private val userSettingsMapper: NewUserSettingsMapper,
    private val userSettingsPersistentStateComponent: UserSettingsPersistentStateComponent,
    private val credentialsServiceRepository: TranslationCredentialsServiceRepository,
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
                ?.removePrefix(projectBasePath)
                ?.removePrefix(projectBasePathBackslash)
                //sometimes theres something left
                ?.removePrefix("\\")
                ?.removePrefix("/")
        )
    }

    /**
     * if there is no settings, we provide default user settings
     */
    override fun getSettings(): UserSettings {
        val toEntity = userSettingsMapper.toEntity(userSettingsPersistentStateComponent.state)
        val key = credentialsServiceRepository.getKey()
        //TODO need to separate this
        return toEntity.copy(openAIKey = key)
    }
    /**
     * wipe the user settings
     */
    override fun wipeUserSettings() {
        userSettingsPersistentStateComponent.setNewState(UserSettingsPersistentStateComponent.IdeaUserSettingsModel())
    }
}
