package de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.PartialFileTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity.TranslateFileContext

fun interface PartialFileResponseHandler {
    fun handlePartialFileResponse(context: TranslateFileContext, partialFileTranslationResponse: PartialFileTranslationResponse)
}
