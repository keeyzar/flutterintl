package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.repository

import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.ExistingTranslation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language

interface ExistingTranslationRepository<T> {
    fun getExistingTranslation(reference: T, baseLanguage: Language, key: String): ExistingTranslation?
}