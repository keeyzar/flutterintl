package de.keeyzar.gpthelper.gpthelper.features.shared.presentation.dto

data class UserSettingsDTO(
    /**
     * where is the directory containing the arb files
     */
    var arbDir: String = "",
    /**
     * how is the key names (e.g. S, AppLocalizations)
     */
    var outputClass: String = "",
    /**
     * whether we're working with a nullable getter
     */
    var nullableGetter: Boolean = true,
    /**
     * base file for the translations, i.e. here you can find the prefix
     */
    var templateArbFile: String = "",
    var intlConfigFile: String = "",
    var watchIntlConfigFile: Boolean = true,
    /**
     * here you can find the file, where the outputClass is located in
     */
    var outputLocalizationFile: String = ""
)
