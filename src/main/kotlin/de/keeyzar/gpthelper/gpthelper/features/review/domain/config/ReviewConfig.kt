package de.keeyzar.gpthelper.gpthelper.features.review.domain.config

import java.net.URI

data class ReviewConfig(
    val timeBetweenReviewRequestsInSeconds: Int = TIME_BETWEEN_REVIEW_REQUESTS_IN_SECONDS,
    val minimumTranslationsBeforeWeAskForReview: Int = MINIMUM_TRANSLATIONS_BEFORE_WE_ASK_FOR_REVIEW,
    val uriFromPlugin: URI = URI_FROM_PLUGIN,
) {
    companion object {
        //is there any cool utilities function for this?
        private const val TWO_DAYS_IN_SECONDS = 2 * 24 * 60 * 60
        private const val TIME_BETWEEN_REVIEW_REQUESTS_IN_SECONDS = 10
        private const val MINIMUM_TRANSLATIONS_BEFORE_WE_ASK_FOR_REVIEW = TWO_DAYS_IN_SECONDS
        private val URI_FROM_PLUGIN = URI("https://plugins.jetbrains.com/plugin/21732-gpt-flutter-intl/reviews")
    }
}
