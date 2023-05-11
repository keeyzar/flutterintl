package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.TaskAmountCalculator
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext

class TranslateKeyTaskAmountCalculator : TaskAmountCalculator {
    override fun calculate(context: TranslateKeyContext): Int {
        return context.availableLanguages.size
    }
}
