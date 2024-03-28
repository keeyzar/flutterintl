package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

/**
 * contains information for changing a single translation
 */
class ChangeTranslationContext(
    var key: String,
    var value: String,
    var description: String,
)