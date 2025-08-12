package de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.FileTranslationRequest

class FileTranslationRequestMapper(
    private val objectMapper: ObjectMapper,
) {
    fun mapToContentString(fileTranslationRequest: FileTranslationRequest): List<String> {
        TODO()
    }
}
