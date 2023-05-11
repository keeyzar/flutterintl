package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

/**
 * ensure that all settings are set, which are required to fulfill the current translation request
 */
fun interface VerifyTranslationSettingsService {
    /**
     * whether the settings are set, if not, the user should be informed / asked to set them
     */
    fun verifySettingsAndInformUserIfInvalid(): Boolean
}
