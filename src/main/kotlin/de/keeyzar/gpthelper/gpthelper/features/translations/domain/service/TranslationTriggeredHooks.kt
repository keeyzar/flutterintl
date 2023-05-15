package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation

/**
 * different kind of translation process hooks
 */
interface TranslationTriggeredHooks {
    /**
     * when you have a single key translation and want it to be triggered
     * combines [translationTriggeredInit] + [translationTriggeredPartial]
     */
    fun translationTriggered(translation: Translation)

    /**
     * when you have multiple keys, which must be replaced
     */
    fun translationTriggeredPartial(translation: Translation)

    /**
     * If there is some kind of initial task to be done once, which is kinda expensive
     */
    fun translationTriggeredInit()
}
