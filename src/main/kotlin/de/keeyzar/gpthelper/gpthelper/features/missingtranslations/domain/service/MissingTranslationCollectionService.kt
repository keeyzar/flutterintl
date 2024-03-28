package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationContext

interface MissingTranslationCollectionService<T> {
    fun collectMissingTranslations(missingTranslationContext: MissingTranslationContext<T>): MissingTranslationContext<T>
}