package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

data class UserTranslationRequest (
    /**
     * the languages to translate to
     */
    val targetLanguages: List<Language>,
    /**
     * the base translation to translate from
     */
    val baseTranslation: Translation,
) {
}
