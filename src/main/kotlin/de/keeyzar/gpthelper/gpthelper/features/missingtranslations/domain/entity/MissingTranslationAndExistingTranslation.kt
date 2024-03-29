package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity

/**
 * contains the missing translations and the corresponding existing translation which the user wants to translate
 */
data class MissingTranslationAndExistingTranslation(
    val missingTranslation: MissingTranslation,
    var existingTranslation: ExistingTranslation?
)
