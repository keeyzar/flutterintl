package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

/**
 * single entry in a translation file
 */
data class SimpleTranslationEntry(
    //used as some kind of identifier
    //TODO remove the nullable id, translation process is the culprit
    val id: String?,
    val desiredKey: String,
    val desiredValue: String,
    val desiredDescription: String,
) {

}
