package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.UserSettingsCorruptException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.UserSettingsMissingException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.UserSettingsMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.model.UserSettingsModel

class PropertiesUserSettingsRepository(
    private val objectMapper: ObjectMapper,
    private val currentProjectProvider: CurrentProjectProvider,
    private val userSettingsMapper: UserSettingsMapper,
) : UserSettingsRepository {
    companion object {
        const val KEY_GPT_HELPER_USER_SETTINGS = "de.keeyzar.gpthelper.usersettings"
    }

    override fun overrideSettings(callback: (oldUserSettings: UserSettings) -> UserSettings) {
        val newSettings = callback(getSettings())
        persistSettings(newSettings)
    }

    override fun saveSettings(userSettings: UserSettings) {
        persistSettings(userSettings)
    }

    private fun persistSettings(userSettings: UserSettings) {
        val properties = PropertiesComponent.getInstance()
        val sanitized = sanitize(userSettings);
        val model = userSettingsMapper.toModel(sanitized);
        WriteAction.runAndWait<Throwable> {
            properties.setValue(KEY_GPT_HELPER_USER_SETTINGS, objectMapper.writeValueAsString(model))
        }
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
        val properties = PropertiesComponent.getInstance()
        val userSettingsString = ReadAction.compute<String, Throwable> {
            properties.getValue(KEY_GPT_HELPER_USER_SETTINGS)
        }
        return if (userSettingsString != null) {
            try {
                val model = objectMapper.readValue(userSettingsString, UserSettingsModel::class.java)
                userSettingsMapper.toEntity(model)
            } catch (e: Throwable) {
                throw UserSettingsCorruptException("Found user settings, but could not parse them", e)
            }
        } else {
            throw UserSettingsMissingException("No user settings found in properties with key $KEY_GPT_HELPER_USER_SETTINGS")
        }
    }

    /**
     * wipe the user settings
     */
    override fun wipeUserSettings() {
        val properties = PropertiesComponent.getInstance()
        WriteAction.runAndWait<Throwable> {
            properties.unsetValue(KEY_GPT_HELPER_USER_SETTINGS)
        }
    }
}
