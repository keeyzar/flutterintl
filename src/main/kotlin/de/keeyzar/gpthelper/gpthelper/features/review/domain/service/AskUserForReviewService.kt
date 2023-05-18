package de.keeyzar.gpthelper.gpthelper.features.review.domain.service

import de.keeyzar.gpthelper.gpthelper.features.review.domain.entity.AskUserForReviewResult

fun interface AskUserForReviewService {
    fun askUserForReview(): AskUserForReviewResult?
}
