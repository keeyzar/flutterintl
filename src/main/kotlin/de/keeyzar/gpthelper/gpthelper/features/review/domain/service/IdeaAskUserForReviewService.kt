package de.keeyzar.gpthelper.gpthelper.features.review.domain.service

import com.intellij.openapi.application.ApplicationManager
import de.keeyzar.gpthelper.gpthelper.features.review.domain.entity.AskUserForReviewResult
import de.keeyzar.gpthelper.gpthelper.features.review.presentation.widgets.ReviewDialog
import java.util.concurrent.atomic.AtomicReference

class IdeaAskUserForReviewService : AskUserForReviewService {
    override fun askUserForReview(): AskUserForReviewResult? {
        val result: AtomicReference<AskUserForReviewResult> = AtomicReference(null);
        ApplicationManager.getApplication().invokeAndWait {
            val dialog = ReviewDialog()
            val closedWithOk = dialog.showAndGet()
            if (closedWithOk) {
                result.set(dialog.getResult())
            } else {
                //we assume he said "ask me later"
                result.set(AskUserForReviewResult(closedWithShouldAskLater = true, closedWithReview = false, closedWithDontAskAgain = false))
            }
        }
        return result.get()
    }
}
