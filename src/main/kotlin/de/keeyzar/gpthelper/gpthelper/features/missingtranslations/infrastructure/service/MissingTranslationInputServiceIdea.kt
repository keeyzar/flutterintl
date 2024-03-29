package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationFilteredTargetTranslation
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationAndExistingTranslation
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.service.MissingTranslationInputService
import java.util.UUID

class MissingTranslationInputServiceIdea : MissingTranslationInputService {
    override fun collectMissingTranslationInput(missingTranslationAndExistingTranslations: List<MissingTranslationAndExistingTranslation>): List<MissingTranslationFilteredTargetTranslation> {
        //we need to collect the missing translations from the user
        //for now we just assume that the user wants to translate to all languages
        return missingTranslationAndExistingTranslations.map {
            MissingTranslationFilteredTargetTranslation(
                it,
                it.missingTranslation.languagesMissing,
                uuid = UUID.randomUUID().toString()
            )
        }
    }
}