package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.fasterxml.jackson.databind.ObjectMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.FileToTranslate
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.FormatTranslationFileContentService

class ArbFormatTranslationFileContentService(
  private val objectMapper: ObjectMapper
) : FormatTranslationFileContentService {
    override fun formatTranslationFileContent(fileToTranslate: FileToTranslate): FileToTranslate {
        val newContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(objectMapper.readTree(fileToTranslate.content))
        return fileToTranslate.copy(content = newContent)
    }
}
