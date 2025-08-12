package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry

data class MultiKeyTranslationContext(
    /**
     * which entries must be modified? We do not need any id anymore whatsoever, because we have keys already
     */
    val translationEntries: List<SimpleTranslationEntry>,
    val baseLanguage: Language,
    val targetLanguages: List<Language>
)
