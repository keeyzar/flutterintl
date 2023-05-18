package de.keeyzar.gpthelper.gpthelper.features.review.domain.repository

import de.keeyzar.gpthelper.gpthelper.features.review.domain.entity.ReviewSettings

interface ReviewRepository {
    fun getReviewSettings(): ReviewSettings
    fun saveReviewSettings(reviewSettings: ReviewSettings)
}
