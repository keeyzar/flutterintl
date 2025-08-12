package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language

/**
 * the missing translation e.g. "greeting" for de and pl - the user said he wants to translate it to de only
 */
data class MissingTranslationFilteredTargetTranslation(
    val missingTranslationAndExistingTranslation: MissingTranslationAndExistingTranslation,
    val languagesToTranslateTo: List<Language>,
    val uuid: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MissingTranslationFilteredTargetTranslation) return false
        return uuid == other.uuid
    }

    override fun hashCode(): Int {
        return uuid.hashCode()
    }
}