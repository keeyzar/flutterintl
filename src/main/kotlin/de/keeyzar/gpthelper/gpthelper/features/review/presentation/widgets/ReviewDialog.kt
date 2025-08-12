package de.keeyzar.gpthelper.gpthelper.features.review.presentation.widgets

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import de.keeyzar.gpthelper.gpthelper.features.review.domain.entity.AskUserForReviewResult
import javax.swing.Action
import javax.swing.JComponent

class ReviewDialog : DialogWrapper(true) {

    private var closedWithReview = false
    private var closedWithShouldAskLater = false
    private var closedWithDontAskAgain = false

    init {
        title = "GPT Flutter Review"
        super.init()
    }

    /**
     * remove the default actions
     */
    override fun createActions(): Array<Action> {
        return emptyArray()
    }

    override fun createCenterPanel(): JComponent {
        return panel {
            row {
                text(
                    "Hello there, fellow developer!<br> I'm Keeyzar, the creator of this plugin for Dart. 🚀<br>" +
                            "Could I kindly ask for a moment of your time to rate this plugin on the marketplace?<br><br>" +
                            "Your valuable feedback will not only fuel my motivation but also contribute greatly to enhancing the plugin's functionality.<br>" +
                            "Your support means the world to me, and I deeply appreciate your willingness to help shape the future of this plugin.<br><br>" +
                            "Thank you immensely!"
                ).align(AlignX.CENTER)
            }
            row {
                panel {
                    row {
                        button("Review") {
                            closedWithReview = true
                            close(0)
                        }
                        button("Ask Me Later") {
                            closedWithShouldAskLater = true
                            close(0)
                        }
                        button("Don't Ask Again") {
                            closedWithDontAskAgain = true
                            close(0)
                        }
                    }
                }.align(AlignX.RIGHT)

                //all buttons to the right
            }
        }
    }

    fun getResult(): AskUserForReviewResult {
        return AskUserForReviewResult(
            closedWithReview = closedWithReview,
            closedWithShouldAskLater = closedWithShouldAskLater,
            closedWithDontAskAgain = closedWithDontAskAgain,
        )
    }


}
