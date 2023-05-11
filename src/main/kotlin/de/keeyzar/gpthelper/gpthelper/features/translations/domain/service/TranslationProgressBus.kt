package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress

interface TranslationProgressBus {
    fun pushPercentage(translationProgress: TranslationProgress)
}
