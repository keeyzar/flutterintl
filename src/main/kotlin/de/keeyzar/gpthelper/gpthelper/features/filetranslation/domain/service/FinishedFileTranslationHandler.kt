package de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity.TranslateFileContext

fun interface FinishedFileTranslationHandler {
    fun finishedTranslation(context: TranslateFileContext)
}
