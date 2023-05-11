package de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity.TranslateFileContext
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.FinishedFileTranslationHandler
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.JsonUtils
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.FormatTranslationFileContentService

class FlutterArbTranslateFileFinished(
    private val translationFileRepository: TranslationFileRepository,
    private val formatTranslationFileContentService: FormatTranslationFileContentService,
    private val jsonUtils: JsonUtils,
) : FinishedFileTranslationHandler {
    override fun finishedTranslation(context: TranslateFileContext) {
        //get base language as string
        val base = translationFileRepository.getTranslationFileByLanguage(context.baseLanguage)
        val target = translationFileRepository.getTranslationFileByLanguage(context.targetLanguage)
        val newTargetString = jsonUtils.reorderJson(base.content, target.content)
        var newTarget = target.copy(content = newTargetString)
        newTarget = formatTranslationFileContentService.formatTranslationFileContent(newTarget)
        translationFileRepository.saveTranslationFile(newTarget)
    }
}
