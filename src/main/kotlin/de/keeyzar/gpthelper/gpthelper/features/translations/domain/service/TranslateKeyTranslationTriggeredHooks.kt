package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service.FlutterArbCurrentFileModificationService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.FlutterGenCommandProcessService

/**
 * implements the domain service for the external translation process
 */
class TranslateKeyTranslationTriggeredHooks(
    private val flutterGenCommandProcessService: FlutterGenCommandProcessService,
    private val currentFileModificationService: FlutterArbCurrentFileModificationService
) : TranslationTriggeredHooks {
    override fun translationTriggered(translation: Translation) {
        translationTriggeredPartial(translation)
        translationTriggeredPostTranslation()
    }

    override fun translationTriggeredPostTranslation() {
        flutterGenCommandProcessService.postTranslationProcess()
    }

    override fun translationTriggeredPartial(translation: Translation) {
        currentFileModificationService.modifyCurrentFile(translation)
    }

    override fun translationTriggeredInit() {
    }
}
