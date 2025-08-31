package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.TranslationTaskBackgroundProgress

data class MultiKeyTranslationContext(
    /**
     * which entries must be modified? We do not need any id anymore whatsoever, because we have keys already
     */
    val translationEntries: List<SimpleTranslationEntry>,
    val baseLanguage: Language,
    val targetLanguages: List<Language>,
    val uuid: String,
    var finished: Boolean = false,
    var taskAmount: Int = Integer.MAX_VALUE,
    var finishedTasks: Int = 0,
    var cancelled: Boolean = false,
) : TranslationTaskBackgroundProgress.TranslationProgressContext {
    override fun getId(): String {
        return uuid
    }

    override fun isFinished(): Boolean {
        return finished
    }

    override var progressText: String = ""

    override fun isCancelled(): Boolean {
        return cancelled
    }

    override fun setCancelled() {
        cancelled = true
    }
}
