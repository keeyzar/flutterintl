package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationInput

fun interface PostTranslationTriggerService {
    fun translationTriggered(context: TranslateKeyContext, userTranslationInput: UserTranslationInput)
}
