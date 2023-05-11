package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext

/**
 * gather information required to request the information from the user
 */
fun interface GatherTranslationContextService {
    fun gatherTranslationContext(): TranslateKeyContext
}
