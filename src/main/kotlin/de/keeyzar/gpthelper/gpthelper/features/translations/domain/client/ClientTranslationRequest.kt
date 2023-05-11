package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation

data class ClientTranslationRequest (
    /**
     * the languages to translate to
     */
    val targetLanguages: List<Language>,
    /**
     * this is the base translation to translate from
     */
    val translation: Translation,
) {
}
