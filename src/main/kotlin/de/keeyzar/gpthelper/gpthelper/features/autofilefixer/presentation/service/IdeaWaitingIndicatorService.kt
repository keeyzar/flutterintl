package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.WaitingIndicatorService
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.ContextProvider
import java.util.*

class IdeaWaitingIndicatorService(
    private val contextProvider: ContextProvider,
)
    : WaitingIndicatorService {
    private var progressIndicator: ProgressIndicator? = null

    override fun startWaiting(uuid: UUID, title: String, description: String) {
        val project = contextProvider.getAutoLocalizeContext(uuid)!!.project
        val task = object : Task.Modal(project, title, true) {
            override fun run(indicator: ProgressIndicator) {
                progressIndicator = indicator
                indicator.isIndeterminate = true
                indicator.text = description
                while (!indicator.isCanceled) {
                    // Do nothing and keep waiting
                    Thread.sleep(100)
                }
            }
        }

        // Ensure we run on the main event dispatch thread
        ApplicationManager.getApplication().invokeLater {
            ProgressManager.getInstance().run(task)
        }
    }

    override fun stopWaiting() {
        // Ensure we run on the main event dispatch thread
        ApplicationManager.getApplication().invokeLater({
            progressIndicator?.cancel()
            progressIndicator = null
        }, ModalityState.any()) //any, because the modal is blocking the EDT
    }

    override fun updateProgress(uuid: UUID, progressText: String) {
        ApplicationManager.getApplication().invokeLater({
            progressIndicator?.text = progressText
        }, ModalityState.any())
    }
}
