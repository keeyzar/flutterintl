package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.TranslationFileModificationException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.UserSettingsException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import java.util.concurrent.Semaphore

class ArbFileModificationService(
    private val translationFileRepository: TranslationFileRepository,
    private val contentModificationService: ContentModificationService,
    private val formatTranslationFileContentService: FormatTranslationFileContentService,
) {
    companion object {
        private val concurrentFileTranslations = Semaphore(1)
    }

    /**
     * the file is modified by adding the new entry, which, in this case, is json
     *
     * @throws TranslationFileModificationException when the file to modify is not found, or is a directory, or can't be edited...
     */
    @Throws(UserSettingsException::class, TranslationFileModificationException::class)
    fun addSimpleTranslationEntry(translation: Translation) {
        concurrentFileTranslations.acquire()
        try {
            val fileToTranslate = translationFileRepository.getTranslationFileByLanguage(translation.lang);
            val newFile = contentModificationService.appendTranslation(fileToTranslate, translation);
            translationFileRepository.saveTranslationFile(newFile);
        } finally {
            concurrentFileTranslations.release()
        }
    }

    /**
     * because we don't exactly know how the structure looks like of the file to translate, e.g. not necessarily json, we move the actual
     * modification out to the infrastructure
     */
    @Throws(UserSettingsException::class, TranslationFileModificationException::class)
    fun replaceSimpleTranslationEntry(translation: Translation) {
        concurrentFileTranslations.acquire()
        try {
            val fileToTranslate = translationFileRepository.getTranslationFileByLanguage(translation.lang);
            var newFile = contentModificationService.replaceWithNewTranslation(fileToTranslate, translation);
            newFile = formatTranslationFileContentService.formatTranslationFileContent(newFile);
            translationFileRepository.saveTranslationFile(newFile);
        } finally {
            concurrentFileTranslations.release()
        }
    }
}
