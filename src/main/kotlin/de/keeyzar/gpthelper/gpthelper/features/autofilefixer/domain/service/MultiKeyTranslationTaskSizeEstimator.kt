package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.MultiKeyTranslationContext

fun interface MultiKeyTranslationTaskSizeEstimator {
    fun estimateTaskSize(multiKeyTranslationContext: MultiKeyTranslationContext): Int
}
