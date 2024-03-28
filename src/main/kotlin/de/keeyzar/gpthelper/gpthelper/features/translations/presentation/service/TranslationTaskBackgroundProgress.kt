package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.TranslationProgressChangeNotifier
import kotlinx.coroutines.runBlocking
import kotlin.math.max

/**
 * listen for changes in the translation progress and update the UI accordingly
 */
class TranslationTaskBackgroundProgress {

    interface TranslationProgressContext {
        fun getId(): String
        fun isFinished(): Boolean
        var progressText: String

        fun isCancelled(): Boolean
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
                    callback()
                }
                while (!finished) {
                    if(translationContext.isCancelled()) {
                        break
                    }
                    try {
                        Thread.sleep(1000)
                    } catch (e: InterruptedException) {
                        Thread.interrupted()
                        println("We were interrupted")
                    }
                }
            }

            fun initProgressListener(progressIndicator: ProgressIndicator) {
                progressIndicator.fraction = 0.0;
                progressIndicator.isIndeterminate = false;
                val con = project.messageBus.connect();
                con.setDefaultHandler { _, objects ->
                    if (objects[0] != null && objects[0] is TranslationProgress) {
                        if (translationContext.getId() != (objects[0] as TranslationProgress).taskId) {
                            //skip, different task
                            return@setDefaultHandler
                        }
                        val progress = objects[0] as TranslationProgress;
                        progressIndicator.fraction = progress.currentTask.toDouble() / max(progress.taskAmount.toDouble(), 1.0)
                        progressIndicator.text2 = "${progress.currentTask}/${progress.taskAmount}"
                        progressIndicator.text = translationContext.progressText
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
                print("finished the task!")
                finishedCallback?.invoke()
            }
        }

        progressManager.runProcessWithProgressAsynchronously(myTask,
            object : BackgroundableProcessIndicator(myTask) {}
        );
    }
}
