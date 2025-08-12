package de.keeyzar.gpthelper.gpthelper.features.review.domain.service

import de.keeyzar.gpthelper.gpthelper.features.review.domain.config.ReviewConfig
import de.keeyzar.gpthelper.gpthelper.features.review.domain.entity.AskUserForReviewResult
import de.keeyzar.gpthelper.gpthelper.features.review.domain.entity.ReviewSettings
import de.keeyzar.gpthelper.gpthelper.features.review.domain.repository.ReviewRepository
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.firstValue
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import java.net.URI
import java.time.OffsetDateTime
import java.time.ZoneOffset

class ReviewServiceTest {
    @Mock
    private lateinit var reviewRepository: ReviewRepository

    @Mock
    private lateinit var askUserForReviewService: AskUserForReviewService

    @Mock
    private lateinit var openPageService: OpenPageService

    @Mock
    private lateinit var reviewConfig: ReviewConfig
    private lateinit var reviewService: ReviewService

    private var testUri = URI("https://www.kevin-kekule.de")

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        reviewService = ReviewService(
            reviewRepository,
            askUserForReviewService,
            openPageService,
            reviewConfig
        )
        `when`(reviewConfig.uriFromPlugin)
            .thenReturn(testUri)
    }

    @Test
    fun `test askUserForReviewIfItIsTime when reviewed is true`() {
        val reviewSettings = ReviewSettings(
            lastReviewRequestTimestamp = null,
            timesReviewRequestSkipped = 0,
            shouldAskLater = false,
            reviewed = true
        )
        `when`(reviewRepository.getReviewSettings()).thenReturn(reviewSettings)

        reviewService.askUserForReviewIfItIsTime()

        verifyNoInteractions(askUserForReviewService, openPageService)
    }

    @Test
    fun `test askUserForReviewIfItIsTime when lastReviewRequestTimestamp is null`() {
        val reviewSettings = ReviewSettings(
            lastReviewRequestTimestamp = null,
            timesReviewRequestSkipped = 0,
            shouldAskLater = false,
            reviewed = false
        )
        `when`(reviewRepository.getReviewSettings()).thenReturn(reviewSettings)
        `when`(askUserForReviewService.askUserForReview()).thenReturn(
            AskUserForReviewResult(
                closedWithReview = false,
                closedWithShouldAskLater = false,
                closedWithDontAskAgain = false
            )
        )
        `when`(reviewConfig.minimumTranslationsBeforeWeAskForReview).thenReturn(1)


        reviewService.askUserForReviewIfItIsTime()
        verifyNoInteractions(askUserForReviewService, openPageService)
    }


    @Test
    fun `test askUserForReviewIfItIsTime when lastReviewRequestTimestamp is null and minmimumTranslations is 0`() {
        val reviewSettings = ReviewSettings(
            lastReviewRequestTimestamp = null,
            timesReviewRequestSkipped = 0,
            shouldAskLater = false,
            reviewed = false
        )
        `when`(reviewRepository.getReviewSettings()).thenReturn(reviewSettings)
        `when`(askUserForReviewService.askUserForReview()).thenReturn(
            AskUserForReviewResult(
                closedWithReview = true,
                closedWithShouldAskLater = false,
                closedWithDontAskAgain = false
            )
        )
        `when`(reviewConfig.minimumTranslationsBeforeWeAskForReview).thenReturn(0)


        reviewService.askUserForReviewIfItIsTime()
        verify(openPageService).openPage(testUri)
        argumentCaptor<ReviewSettings>().apply {
            verify(reviewRepository).saveReviewSettings(capture())
            val call = firstValue
            Assertions.assertThat(call.lastReviewRequestTimestamp).isNotNull()
            Assertions.assertThat(call.reviewed).isTrue()
        }
    }

    @Test
    fun `test askUserForReviewIfItIsTime when lastReviewRequestTimestamp is not null and it's time for review`() {
        val now = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()
        val lastReviewRequestTimestamp = now - reviewConfig.timeBetweenReviewRequestsInSeconds - 1

        val reviewSettings = ReviewSettings(
            lastReviewRequestTimestamp = lastReviewRequestTimestamp,
            timesReviewRequestSkipped = 0,
            shouldAskLater = false,
            reviewed = false
        )
        `when`(reviewRepository.getReviewSettings()).thenReturn(reviewSettings)
        `when`(askUserForReviewService.askUserForReview()).thenReturn(
            AskUserForReviewResult(
                closedWithReview = true,
                closedWithShouldAskLater = false,
                closedWithDontAskAgain = false
            )
        )

        reviewService.askUserForReviewIfItIsTime()

        argumentCaptor<ReviewSettings>().apply {
            verify(reviewRepository).saveReviewSettings(capture())
            val call = firstValue
            Assertions.assertThat(call.lastReviewRequestTimestamp).isNotNull()
            Assertions.assertThat(call.reviewed).isTrue()
        }
    }

    @Test
    fun `test askUserForReviewIfItIsTime when lastReviewRequestTimestamp is not null and it's not time for review`() {
        val now = OffsetDateTime.now(ZoneOffset.UTC).toEpochSecond()
        val lastReviewRequestTimestamp = now - reviewConfig.timeBetweenReviewRequestsInSeconds + 1

        val reviewSettings = ReviewSettings(
            lastReviewRequestTimestamp = lastReviewRequestTimestamp,
            timesReviewRequestSkipped = 0,
            shouldAskLater = false,
            reviewed = false
        )
        `when`(reviewRepository.getReviewSettings()).thenReturn(reviewSettings)

        reviewService.askUserForReviewIfItIsTime()

        verifyNoInteractions(askUserForReviewService, openPageService)
    }
}
