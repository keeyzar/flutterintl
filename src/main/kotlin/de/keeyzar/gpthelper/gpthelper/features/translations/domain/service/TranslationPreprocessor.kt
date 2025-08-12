package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationInput

class TranslationPreprocessor(
    private val verifyTranslationSettingsService: VerifyTranslationSettingsService,
    private val gatherTranslationContextService: GatherTranslationContextService,
    private val gatherUserInputService: GatherUserInputService
) {
    /**
     * preprocess the request.
     * @return true, if preprocessing was successful, false otherwise
     */
    fun preprocess(translationContext: TranslationContext) : Pair<TranslateKeyContext, UserTranslationInput>? {
        //verify settings are set
        val verified = verifyTranslationSettingsService.verifySettingsAndInformUserIfInvalid()
        if (!verified) {
            println("settings are not verified, please set them")
            return null
        }

        val statement = translationContext.changeTranslationContext?.value
        val translateKeyContext = gatherTranslationContextService.gatherTranslationContext(statement)
        translateKeyContext.changeTranslationContext = translationContext.changeTranslationContext

        val userTranslationInput = gatherUserInputService.requestInformationFromUser(translateKeyContext)
        if (userTranslationInput == null) {
            println("user cancelled the process")
            return null
        }
        return Pair(translateKeyContext, userTranslationInput)
    }
}
