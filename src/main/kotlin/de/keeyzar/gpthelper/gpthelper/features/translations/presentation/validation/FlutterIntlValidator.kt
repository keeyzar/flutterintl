package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.validation

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings

class FlutterIntlValidator {
    /**
     * whether or not the settings are valid in general, i.e. anything is set
     * not yet checking for plausibility, i.e. whether the config is actually identical to the flutter gen config
     *
     * TODO would be nice, if we had some kind of cooler validation here, would a validation framework be nice here?
     * I'm not the first one encountering the issue, but never used it in this kind of way, guess I need some kind of
     * "DTO abstraction?"
     */
    fun valid(userSettings: UserSettings): List<String> {
        val errors = mutableListOf<String>()
        if (userSettings.outputClass == "") {
            errors.add("Output class not set")
        }
        if (userSettings.arbDir == "") {
            errors.add("Arb dir not set")
        }
        if (userSettings.templateArbFile == "") {
            errors.add("Template arb file not set")
        }
        if (userSettings.outputLocalizationFile == "") {
            errors.add("Output localization file not set")
        }
        return errors
    }
}
