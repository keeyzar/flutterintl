package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository


class OpenAIUserSettingsRepositoryTestUtil : UserSettingsRepository {
    override fun overrideSettings(callback: (oldUserSettings: UserSettings) -> UserSettings) {
        TODO("Not yet implemented")
    }

    override fun getSettings(): UserSettings {
        return UserSettings(openAIKey = "keys/openai.key", nullableGetter = false, templateArbFile = "", arbDir = "", outputClass = "", outputLocalizationFile = "", intlConfigFile = "")
    }

    override fun saveSettings(userSettings: UserSettings) {
        TODO("Not yet implemented")
    }

    override fun wipeUserSettings() {
        TODO("Not yet implemented")
    }

}
