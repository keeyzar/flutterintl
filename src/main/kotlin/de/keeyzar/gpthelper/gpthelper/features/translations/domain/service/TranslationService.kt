package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.ARBFileContent
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserAddNewLanguageRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslateStringRequest

interface TranslationService {
    suspend fun translate(userTranslateStringRequest: UserTranslateStringRequest): Set<ARBFileContent>
    fun placeholderTranslate(userTranslateStringRequest: UserTranslateStringRequest): Set<ARBFileContent>
    suspend fun translateWholeFile(userAddNewLanguageRequest: UserAddNewLanguageRequest): String
}
