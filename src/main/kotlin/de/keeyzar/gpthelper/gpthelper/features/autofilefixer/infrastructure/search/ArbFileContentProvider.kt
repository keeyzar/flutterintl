package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search

import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository

class ArbFileContentProvider(
    private val arbFilesService: ArbFilesService,
    private val translationFileRepository: TranslationFileRepository,
) {
    fun getBaseLangContent(): String {
        val lang = com.intellij.openapi.application.ReadAction.compute<Language, RuntimeException> {
            arbFilesService.getBaseLanguage(null)
        }
        return translationFileRepository.getTranslationFileByLanguage(lang).content
    }
}