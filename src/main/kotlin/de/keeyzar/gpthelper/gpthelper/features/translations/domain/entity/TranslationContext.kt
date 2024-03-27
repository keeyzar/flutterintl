package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

/**
 * contains information for a single translation
 * @param id the id of the translation, unique, used for identification - is not persisted, a normal uuid is enough
 * @param taskAmountHandled the amount of tasks that have been handled
 */
class TranslationContext(
    val id: String,
    var progressText: String,
    var taskAmount: Int,
    var translationRequest: UserTranslationRequest?,
    var taskAmountHandled: Int,
    var finished: Boolean = false,
    var cancelled: Boolean = false,
) {
}