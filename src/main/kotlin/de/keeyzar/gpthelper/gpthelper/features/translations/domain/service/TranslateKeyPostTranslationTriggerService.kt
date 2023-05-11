package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.CurrentFileModificationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationInput
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.FlutterGenCommandProcessService

/**
 * implements the domain service for the external translation process
 */
class TranslateKeyPostTranslationTriggerService(
    private val flutterGenCommandProcessService: FlutterGenCommandProcessService,
    private val currentFileModificationService: FlutterArbCurrentFileModificationService
) : PostTranslationTriggerService {
    override fun translationTriggered(context: TranslateKeyContext, userTranslationInput: UserTranslationInput) {
        flutterGenCommandProcessService.postTranslationProcess()
        val currentFileModificationContext = CurrentFileModificationContext(userTranslationInput)
        currentFileModificationService.modifyCurrentFile(currentFileModificationContext)
    }
}
