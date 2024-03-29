package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationContext

interface MissingTranslationCollectionService<T> {

    /**
     * Search for missing translations based on the given context and their reference.
     * In intelliJ the reference is a reference to the File, but e.g. in visual studio code it would be something else
     */
    fun collectMissingTranslations(missingTranslationContext: MissingTranslationContext<T>): MissingTranslationContext<T>
}