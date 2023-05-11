package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

/**
 * single entry in a translation file
 */
data class SimpleTranslationEntry(
    val desiredKey: String,
    val desiredValue: String,
    val desiredDescription: String,
) {

}
