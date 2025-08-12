package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

/**
 * in flutter e.g. you need to trigger a process after modification, might not be necessary for different languages
 */
fun interface ExternalTranslationProcessService {
    fun postTranslationProcess()
}
