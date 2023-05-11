package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

fun interface TranslationErrorProcessHandler {
    fun displayErrorToUser(e: Throwable)
}
