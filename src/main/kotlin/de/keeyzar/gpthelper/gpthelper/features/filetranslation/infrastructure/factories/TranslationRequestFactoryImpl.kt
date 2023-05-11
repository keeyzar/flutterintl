package de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.factories

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.FileTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity.TranslateFileContext
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.factories.TranslationRequestFactory
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.presentation.service.IdeaTargetLanguageProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository

class TranslationRequestFactoryImpl(
    private val fileRepository: TranslationFileRepository,
) : TranslationRequestFactory {
    override fun createRequest(translateFileContext: TranslateFileContext): FileTranslationRequest {
        val file = fileRepository.getTranslationFileByLanguage(translateFileContext.baseLanguage)
        return FileTranslationRequest(
            content = file.content,
            targetLanguage = translateFileContext.targetLanguage,
            baseLanguage = translateFileContext.baseLanguage,
        )
    }
}
