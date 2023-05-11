package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation

/**
 * represents all translations for all files, because the original translation might be modified because
 * of wording etc.
 */
class PartialTranslationResponse(var translation: Translation) {

    fun getTranslationKey(): String {
        return translation.entry.desiredKey
    }

    fun getTargetLanguage(): Language {
        return translation.lang
    }
}
