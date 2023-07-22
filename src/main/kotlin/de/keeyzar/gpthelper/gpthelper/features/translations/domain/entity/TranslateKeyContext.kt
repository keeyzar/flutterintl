package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

/**
 * contains information about the current request to translate a key,
 * though from the perspective of us, asking the user for information
 *
 */
data class TranslateKeyContext(
    /**
     * the current statement to be translated
     * most of the time a StringLiteral, but might be something different in the future
     */
    val statement: String,
    /**
     * which is the language this translation stems from?
     */
    val baseLanguage: Language,
    /**
     * we might want to show user information based on the last input
     */
    val lastUserInput: List<UserTranslationInput>?,
    val availableLanguages: List<Language>
) {
}
