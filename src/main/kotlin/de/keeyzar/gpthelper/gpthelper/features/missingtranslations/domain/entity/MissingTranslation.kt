package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language

/**
 * e.g. "greeting" might be missing in "de, pl, fr"
 */
data class MissingTranslation(
    val key: String,
    var languagesMissing : List<Language>,
)
