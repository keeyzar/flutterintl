package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

/**
 * contains information about the translation
 */
data class UserTranslationInput(
    /**
     * the desired key of the translation
     */
    val desiredKey: String,
    /**
     * the desired value of the translation
     */
    val desiredValue: String,
    /**
     * the desired description
     */
    val desiredDescription: String,
    /**
     * normally, the user wants to translate all languages
     */
    val languagesToTranslate: Map<String, Boolean>
) {
}
