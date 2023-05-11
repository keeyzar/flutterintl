package de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationInput

interface UserTranslationInputRepository {
    fun appendTranslationDialogData(userTranslationInput: UserTranslationInput)
    fun saveTranslationDialogData(userTranslationDialogData: List<UserTranslationInput>)
    fun getLatestTranslationDialogData(): UserTranslationInput?
    fun getAllTranslationDialogData(): List<UserTranslationInput>

}
