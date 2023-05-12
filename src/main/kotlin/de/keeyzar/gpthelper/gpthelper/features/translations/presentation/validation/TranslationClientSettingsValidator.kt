package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.validation

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationCredentialsServiceRepository

class TranslationClientSettingsValidator(
    private val credentialsServiceRepository: TranslationCredentialsServiceRepository,
) {
    /**
     * whether or not the settings are valid in general, i.e. anything is set
     * not yet checking for plausibility, i.e. whether the config is actually identical to the flutter gen config
     *
     * TODO would be nice, if we had some kind of cooler validation here, would a validation framework be nice here?
     * I'm not the first one encountering the issue, but never used it in this kind of way, guess I need some kind of
     * "DTO abstraction?"
     */
    fun valid(): List<String> {
        return when (credentialsServiceRepository.getKey()) {
            null -> listOf("No key set")
            else -> listOf()
        }
    }
}
