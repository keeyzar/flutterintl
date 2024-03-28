package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationTargetTranslation
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationWithExistingTranslation
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.service.MissingTranslationInputService
import java.util.UUID

class MissingTranslationInputServiceIdea : MissingTranslationInputService {
    override fun collectMissingTranslationInput(missingTranslationWithExistingTranslations: List<MissingTranslationWithExistingTranslation>): List<MissingTranslationTargetTranslation> {
        //we need to collect the missing translations from the user
        //for now we just assume that the user wants to translate to all languages
        return missingTranslationWithExistingTranslations.map {
            MissingTranslationTargetTranslation(
                it,
                it.missingTranslation.languagesMissing,
                uuid = UUID.randomUUID().toString()
            )
        }
    }
}