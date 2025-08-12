package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.FileToTranslate
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation

interface ContentModificationService {
    fun appendTranslation(fileToTranslate: FileToTranslate, translation: Translation) : FileToTranslate
    fun replaceWithNewTranslation(fileToTranslate: FileToTranslate, translation: Translation) : FileToTranslate
}
