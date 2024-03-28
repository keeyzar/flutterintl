package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity

/**
 * the translation that is already present in the arb files
 */
class ExistingTranslation(
    val key: String,
    val value: String,
    val description: String?,
) {
}