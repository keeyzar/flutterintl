package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.TranslationTaskBackgroundProgress

/**
 * contains information for a single translation
 * @param uuid the id of the translation, unique, used for identification - is not persisted, a normal uuid is enough
 * @param taskAmountHandled the amount of tasks that have been handled
 */
class TranslationContext(
    val uuid: String,
    override var progressText: String,
    var taskAmount: Int,
    var translationRequest: UserTranslationRequest?,
    var taskAmountHandled: Int,
    var finished: Boolean = false,
    var cancelled: Boolean = false,
    var changeTranslationContext: ChangeTranslationContext? = null
) : TranslationTaskBackgroundProgress.TranslationProgressContext {

    override fun getId(): String {
        return uuid
    }
    override fun isFinished(): Boolean {
        return finished
    }

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled() {
        cancelled = true
    }
}