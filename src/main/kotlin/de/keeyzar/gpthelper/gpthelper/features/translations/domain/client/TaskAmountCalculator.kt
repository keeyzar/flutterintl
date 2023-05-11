package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext

/**
 * Because the translation is broken down into multiple tasks, and we don't know how many tasks, we need to get the information
 */
fun interface TaskAmountCalculator {
    fun calculate(context: TranslateKeyContext): Int
}
