package de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.FileToTranslate
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import java.nio.file.Path

/**
 * this repository is for the domain representation of a translation file.
 */
interface TranslationFileRepository {
    fun getTranslationFileByLanguage(language: Language): FileToTranslate
    fun saveTranslationFile(fileToTranslate: FileToTranslate)
    fun getPathsToTranslationFiles(): List<Path>
}
