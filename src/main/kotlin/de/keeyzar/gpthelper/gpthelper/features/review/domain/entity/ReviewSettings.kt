package de.keeyzar.gpthelper.gpthelper.features.review.domain.entity

data class ReviewSettings(
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
    /**
     * if the user has already reviewed, or does not wish to review
     */
    val reviewed: Boolean,
)
