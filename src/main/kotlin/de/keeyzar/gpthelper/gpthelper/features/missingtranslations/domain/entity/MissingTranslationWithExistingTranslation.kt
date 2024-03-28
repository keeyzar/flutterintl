package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity

data class MissingTranslationWithExistingTranslation(
    val missingTranslation: MissingTranslation,
    var existingTranslation: ExistingTranslation?
)
