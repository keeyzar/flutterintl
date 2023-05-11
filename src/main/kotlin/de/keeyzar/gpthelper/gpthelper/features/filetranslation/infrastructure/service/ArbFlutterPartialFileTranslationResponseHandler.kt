package de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.PartialFileTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity.TranslateFileContext
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.PartialFileResponseHandler
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.FormatTranslationFileContentService

class ArbFlutterPartialFileTranslationResponseHandler(
    private val arbFileContentModificationService: ArbFileContentModificationService,
    private val translationFileRepository: TranslationFileRepository,
) : PartialFileResponseHandler {
    override fun handlePartialFileResponse(context: TranslateFileContext, partialFileTranslationResponse: PartialFileTranslationResponse) {
        var file = translationFileRepository.createOrGetTranslationFileByLanguage(context.targetLanguage)
        file = arbFileContentModificationService.appendTranslation(file, partialFileTranslationResponse.entry)
        translationFileRepository.saveTranslationFile(file)
    }
}
