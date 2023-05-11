package de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language

fun interface TargetLanguageProvider {
    fun getTargetLanguage(): Language?
}
