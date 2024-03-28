package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity

import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.TranslationTaskBackgroundProgress

class MissingTranslationContext<T>(
    val uuid: String,
    val missingTranslations: List<MissingTranslation>,
    var missingTranslationTargetTranslations: List<MissingTranslationTargetTranslation>? = null,
    var reference: T,
    var finished: Boolean = false,
    var taskAmount: Int = Integer.MAX_VALUE,
    var finishedTasks: Int = 0,
    var cancelled: Boolean = false,
    var translationsWithIssues: List<MissingTranslationTargetTranslation>? = null,
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
}
