package de.keeyzar.gpthelper.gpthelper.features.review.infrastructure.model

class ReviewSettingsModel(
    /**
     * so we can ensure to ask only once a week or something like that
     */
    val lastReviewRequestTimestamp: Long?,
    /**
     * how many times the user
     */
    val timesReviewRequestSkipped: Long,
    /**
     * if the user has not yet answered on this one
     */
    val shouldAskLater: Boolean?,
    val reviewed: Boolean,
) {
    companion object {
        val DEFAULT: ReviewSettingsModel = ReviewSettingsModel(
            lastReviewRequestTimestamp = null,
            timesReviewRequestSkipped = 0,
            shouldAskLater = null,
            reviewed = false,
        )
    }
}
