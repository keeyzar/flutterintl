package de.keeyzar.gpthelper.gpthelper.features.review.infrastructure.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import de.keeyzar.gpthelper.gpthelper.features.review.domain.entity.ReviewSettings
import de.keeyzar.gpthelper.gpthelper.features.review.domain.repository.ReviewRepository
import de.keeyzar.gpthelper.gpthelper.features.review.infrastructure.mapper.ReviewSettingsMapper
import de.keeyzar.gpthelper.gpthelper.features.review.infrastructure.model.ReviewSettingsModel

class IdeaReviewRepository(
    private val reviewSettingsMapper: ReviewSettingsMapper,
    private val objectMapper: ObjectMapper,
) : ReviewRepository {
    private var cachedReviewSettings: ReviewSettings? = null

    companion object {
        const val REVIEW_SETTINGS_KEY = "REVIEW_SETTINGS"
    }

    override fun getReviewSettings(): ReviewSettings {
        val tmp = cachedReviewSettings
        if (tmp != null) {
            return tmp
        }

        val settingsString = ReadAction.compute<String, Throwable> {
            PropertiesComponent.getInstance().getValue(REVIEW_SETTINGS_KEY, "")
        }

        val settingsModel: ReviewSettingsModel = if (settingsString == "") {
            ReviewSettingsModel.DEFAULT
        } else {
            try {
                objectMapper.readValue(settingsString, ReviewSettingsModel::class.java)
            } catch (t: Throwable) {
                println("Well, we had some issue loading the reviewSettingsModel from a json object, therefore we return the default representation - sorry for disturbing you again!")
                t.printStackTrace()
                ReviewSettingsModel.DEFAULT
            }
        }
        return reviewSettingsMapper.toEntity(settingsModel)
    }

    override fun saveReviewSettings(reviewSettings: ReviewSettings) {
        cachedReviewSettings = reviewSettings
        val settingsModel = reviewSettingsMapper.toModel(reviewSettings)
        val settingsString = objectMapper.writeValueAsString(settingsModel)

        WriteAction.runAndWait<Throwable> {
            PropertiesComponent.getInstance().setValue(REVIEW_SETTINGS_KEY, settingsString)
        }
    }
}
