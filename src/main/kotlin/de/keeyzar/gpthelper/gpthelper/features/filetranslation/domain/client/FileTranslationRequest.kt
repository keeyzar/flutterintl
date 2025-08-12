package de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language

data class FileTranslationRequest(
    val content: String,
    val targetLanguage: Language,
    val baseLanguage: Language,
)
