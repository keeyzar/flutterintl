package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.FileToTranslate

fun interface FormatTranslationFileContentService {
    fun formatTranslationFileContent(fileToTranslate: FileToTranslate): FileToTranslate
}
