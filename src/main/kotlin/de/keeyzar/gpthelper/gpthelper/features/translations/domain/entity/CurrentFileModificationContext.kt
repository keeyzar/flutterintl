package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

/**
 * what is required to modify the current file, i.e. fix imports, fix current statement
 */
data class CurrentFileModificationContext(
    val userTranslationInput: UserTranslationInput,
)
