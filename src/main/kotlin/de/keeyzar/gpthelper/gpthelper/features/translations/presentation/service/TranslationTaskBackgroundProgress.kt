package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import de.keeyzar.gpthelper.gpthelper.common.error.GeneralErrorHandler
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.TranslationProgressChangeNotifier
import kotlinx.coroutines.runBlocking
import java.io.PrintWriter
import java.io.StringWriter
import kotlin.math.max

/**
 * listen for changes in the translation progress and update the UI accordingly
 */
class TranslationTaskBackgroundProgress(val generalErrorHandler: GeneralErrorHandler) {
    var log: Logger = Logger.getInstance(TranslationTaskBackgroundProgress::class.java)

    interface TranslationProgressContext {
        fun getId(): String
        fun isFinished(): Boolean
        var progressText: String

        fun isCancelled(): Boolean
        fun setCancelled()
    }
    /**
     * will call in a blocking context the provided callback.
     * will listen on [TranslationProgressChangeNotifier.CHANGE_ACTION_TOPIC] and update the UI accordingly
     */
    fun <T : TranslationProgressContext> triggerInBlockingContext(
        project: Project,
        callback: suspend () -> Unit,
        translationContext: T,
        finishedCallback: (() -> Unit)? = null,
    ) {
        val progressManager = ProgressManager.getInstance()

        val myTask = object : Task.Backgroundable(project, "Translating...", true) {
            var finished = false
            override fun run(visibleIndicator: ProgressIndicator) {
                initProgressListener(visibleIndicator)
                runBlocking {
                    try {
                        callback()
                    } catch (throwable: Throwable) {
                        generalErrorHandler.handleError(project, throwable)
                        log.error("Error during background task", throwable)
                        finished = true // Stop the task
                    }
                    while (!finished) {
                        log.trace("waiting for the task to finish")
                        if(translationContext.isCancelled()) {
                            break
                        }
                        if(visibleIndicator.isCanceled) {
                            translationContext.setCancelled()
                            break
                        }
                        try {
                            Thread.sleep(1000)
                        } catch (e: InterruptedException) {
                            Thread.interrupted()
                            log.info("We were interrupted")
                        }
                    }
                }

            }

            fun initProgressListener(progressIndicator: ProgressIndicator) {
                progressIndicator.fraction = 0.0
                progressIndicator.isIndeterminate = false
                val con = project.messageBus.connect()
                con.setDefaultHandler { _, objects ->
                    if (objects[0] != null && objects[0] is TranslationProgress) {
                        log.trace("received a message")
                        if (translationContext.getId() != (objects[0] as TranslationProgress).taskId) {
                            //skip, different task
                            return@setDefaultHandler
                        }
                        val progress = objects[0] as TranslationProgress;
                        progressIndicator.fraction = progress.currentTask.toDouble() / max(progress.taskAmount.toDouble(), 1.0)
                        progressIndicator.text2 = "${progress.currentTask}/${progress.taskAmount}"
                        progressIndicator.text = "${translationContext.progressText}: ${progress.currentTask}/${progress.taskAmount}"
                        if (progress.currentTask == progress.taskAmount || translationContext.isFinished()) {
                            finished = true
                        }
                    } else {
                        throw IllegalStateException("Unknown message received");
                    }
                }
                con.subscribe(TranslationProgressChangeNotifier.CHANGE_ACTION_TOPIC)
            }

            override fun onFinished() {
                super.onFinished()
                log.trace("finished the task!")
                finishedCallback?.invoke()
            }
        }

        progressManager.runProcessWithProgressAsynchronously(myTask,
            object : BackgroundableProcessIndicator(myTask) {}
        );
    }
}
