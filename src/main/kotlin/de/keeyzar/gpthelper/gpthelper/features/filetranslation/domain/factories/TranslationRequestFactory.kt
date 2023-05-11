package de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.factories

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.FileTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity.TranslateFileContext

fun interface TranslationRequestFactory {
    fun createRequest(translateFileContext: TranslateFileContext): FileTranslationRequest
}
