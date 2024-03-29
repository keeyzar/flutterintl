package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ReadAction
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationInput
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.translationdialogdata.TranslationDialogDataCorruptException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.translationdialogdata.TranslationDialogDataMissingException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserTranslationInputRepository

class PropertiesUserTranslationInputRepository(
    private val objectMapper: ObjectMapper,
    private val userSettingsRepository: UserSettingsRepository
) : UserTranslationInputRepository {

    companion object {
        private const val PROPERTIES_KEY_TRANSLATION_DIALOG_DATA = "de.keeyzar.gpthelper.translations.translationdialogdata"
    }

    override fun appendTranslationDialogData(userTranslationInput: UserTranslationInput) {
        try {
            val allTranslationDialogData = getAllTranslationDialogData()
            val maxHistoryLength = userSettingsRepository.getSettings().maxTranslationHistory;
            //make a sublist from len - maxHistoryLength to len, when len > maxHistoryLength
            val shortenedHistoryLen = allTranslationDialogData.subList(
                maxOf(allTranslationDialogData.size - maxHistoryLength, 0),
                allTranslationDialogData.size
            )
            //max size of all Translation Dialog Data
            saveTranslationDialogData(shortenedHistoryLen + userTranslationInput)
        } catch (e: TranslationDialogDataMissingException) {
            saveTranslationDialogData(listOf(userTranslationInput))
        }
    }

    override fun saveTranslationDialogData(userTranslationDialogData: List<UserTranslationInput>) {
        val properties = PropertiesComponent.getInstance()
        properties.setValue(PROPERTIES_KEY_TRANSLATION_DIALOG_DATA, objectMapper.writeValueAsString(userTranslationDialogData), "")
    }

    override fun getLatestTranslationDialogData(): UserTranslationInput? {
        return try {
            getAllTranslationDialogData().lastOrNull()
        } catch (e: TranslationDialogDataMissingException) {
            null
        }
    }

    override fun getAllTranslationDialogData(): List<UserTranslationInput> {
        val properties = PropertiesComponent.getInstance()
        val translationDialogDataString = ReadAction.compute<String, Throwable> {
            properties.getValue(PROPERTIES_KEY_TRANSLATION_DIALOG_DATA)
        }
        if (translationDialogDataString != null) {
            try {
                return objectMapper.readValue(
                    translationDialogDataString,
                    objectMapper.typeFactory.constructCollectionType(List::class.java, UserTranslationInput::class.java)
                )
            } catch (e: Exception) {
                throw TranslationDialogDataCorruptException("Found translation dialog data settings, but could not parse them", e)
            }
        } else {
            return emptyList()
        }
    }
}
