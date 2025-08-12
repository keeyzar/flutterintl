package de.keeyzar.gpthelper.gpthelper.features.review.domain.entity

data class AskUserForReviewResult(
    val closedWithReview: Boolean,
    val closedWithShouldAskLater: Boolean,
    val closedWithDontAskAgain: Boolean,
)
