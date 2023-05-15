package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.MultiKeyTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.MultiKeyTranslationTaskSizeEstimator

class OpenAIMultiKeyTranslationTaskSizeEstimator : MultiKeyTranslationTaskSizeEstimator {
    override fun estimateTaskSize(multiKeyTranslationContext: MultiKeyTranslationContext): Int {
        return multiKeyTranslationContext.translationEntries.size * multiKeyTranslationContext.targetLanguages.size
    }
}
