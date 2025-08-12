package de.keeyzar.gpthelper.gpthelper.features.review.domain.service

import de.keeyzar.gpthelper.gpthelper.features.review.domain.config.ReviewConfig
import de.keeyzar.gpthelper.gpthelper.features.review.domain.entity.AskUserForReviewResult
import de.keeyzar.gpthelper.gpthelper.features.review.domain.entity.ReviewSettings
import de.keeyzar.gpthelper.gpthelper.features.review.domain.repository.ReviewRepository
import org.jetbrains.annotations.VisibleForTesting
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val askUserForReviewService: AskUserForReviewService,
    private val openPageService: OpenPageService,
    private val reviewConfig: ReviewConfig,
) {

    /**
     * this method might be blocking!
     * we check whether the user should be asked for a review and show a dialog accordingly
     */
    fun askUserForReviewIfItIsTime() {
        val reviewSettings = reviewRepository.getReviewSettings()
        //for faster access
        val (lastReviewRequestTimestamp, timesReviewRequestSkipped, _, reviewed) = reviewSettings
        if (reviewed) {
            println("we asked for a review already, nothing to do")
            return
        } else if (lastReviewRequestTimestamp == null) {
            println("there was no review yet")
            return if (timesReviewRequestSkipped < reviewConfig.minimumTranslationsBeforeWeAskForReview) {
                val newReviewSettings = reviewSettings.copy(timesReviewRequestSkipped = timesReviewRequestSkipped + 1)
                reviewRepository.saveReviewSettings(newReviewSettings)
            } else {
                //it's time to ask him for a review
                val result = askUserForReviewService.askUserForReview()
                val parsedResult = parseUserResponse(reviewSettings, result)
                reviewRepository.saveReviewSettings(parsedResult.reviewSettings)
                showReviewPageIfRequired(parsedResult)
            }
        } else {
            //check whether last review request is 2 days ago
            val now = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()
            if (now - lastReviewRequestTimestamp > reviewConfig.timeBetweenReviewRequestsInSeconds) {
                //it's time to ask him for a review
                val result = askUserForReviewService.askUserForReview()
                val parsedResult = parseUserResponse(reviewSettings, result)
                reviewRepository.saveReviewSettings(parsedResult.reviewSettings)
                showReviewPageIfRequired(parsedResult)
            } else {
                println("it's not time yet to ask for a review")
            }
        }
    }

    private fun showReviewPageIfRequired(parsedResult: ParsedResponse) {
        if (parsedResult.shouldShowReviewPage) {
            openPageService.openPage(reviewConfig.uriFromPlugin)
        }
    }

    @VisibleForTesting
    fun parseUserResponse(reviewSettings: ReviewSettings, result: AskUserForReviewResult?): ParsedResponse {
        val now = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()

        return when {
            result == null -> {
                //we assume user has said "ask me later"
                val newSettings = reviewSettings.copy(lastReviewRequestTimestamp = now)
                ParsedResponse(false, newSettings)
            }

            result.closedWithReview -> {
                val newSettings = reviewSettings.copy(reviewed = true, lastReviewRequestTimestamp = now)
                ParsedResponse(true, newSettings)

            }

            result.closedWithDontAskAgain -> {
                val newSettings = reviewSettings.copy(reviewed = true, lastReviewRequestTimestamp = now)
                ParsedResponse(false, newSettings)
            }

            result.closedWithShouldAskLater -> {
                val newSettings = reviewSettings.copy(lastReviewRequestTimestamp = now, shouldAskLater = true)
                ParsedResponse(false, newSettings)
            }

            else -> {
                throw IllegalStateException("unknown state")
            }
        }
    }

    class ParsedResponse(
        val shouldShowReviewPage: Boolean,
        val reviewSettings: ReviewSettings,
    )
}
