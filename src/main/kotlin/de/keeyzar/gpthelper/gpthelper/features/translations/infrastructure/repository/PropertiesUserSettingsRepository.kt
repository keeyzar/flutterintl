package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.UserSettingsMapper

class PropertiesUserSettingsRepository(
    private val project: Project,
    private val userSettingsMapper: UserSettingsMapper
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
        UserSettingsPersistentStateComponent.getInstance(project).setNewState(model)
    }

    private fun sanitize(userSettings: UserSettings): UserSettings {
        val projectBasePath = project.basePath ?: ""
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
        val state = UserSettingsPersistentStateComponent.getInstance(project).state
        val settings = userSettingsMapper.toEntity(state)
        if (settings.flutterImportStatement.isBlank()) {
            return settings.copy(flutterImportStatement = "package:flutter_gen/gen_l10n/app_localizations.dart")
        }
        return settings
    }

    /**
     * wipe the user settings
     */
    override fun wipeUserSettings() {
        UserSettingsPersistentStateComponent.getInstance(project).setNewState(UserSettingsPersistentStateComponent.UserSettingsModel())
    }
}
