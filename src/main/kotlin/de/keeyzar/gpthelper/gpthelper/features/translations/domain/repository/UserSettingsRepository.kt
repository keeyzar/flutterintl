package de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings

interface UserSettingsRepository {
    /**
     * provides the old userSettings, so you can modify it, and return it with the copy function
     */
    fun overrideSettings(callback: (oldUserSettings: UserSettings) -> UserSettings)
    fun getSettings(): UserSettings
    fun saveSettings(userSettings: UserSettings)
    fun wipeUserSettings()
}
