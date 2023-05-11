package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.TranslationProgressChangeNotifier
import kotlinx.coroutines.runBlocking
import kotlin.math.max

/**
 * listen for changes in the translation progress and update the UI accordingly
 */
class TranslationTaskBackgroundProgress {

    /**
     * will call in a blocking context the provided callback.
     * will listen on [TranslationProgressChangeNotifier.CHANGE_ACTION_TOPIC] and update the UI accordingly
     */
    fun triggerInBlockingContext(
        project: Project,
        callback: suspend () -> Unit,
    ) {
        val progressManager = ProgressManager.getInstance()

        val myTask = object : Task.Backgroundable(project, "Translating...", true) {
            override fun run(visibleIndicator: ProgressIndicator) {
                initProgressListener(visibleIndicator)
                runBlocking {
                    callback()
                }
            }

            fun initProgressListener(progressIndicator: ProgressIndicator) {
                progressIndicator.fraction = 0.0;
                progressIndicator.isIndeterminate = false;
                val con = project.messageBus.connect();
                con.setDefaultHandler { _, objects ->
                    if (objects[0] != null && objects[0] is TranslationProgress) {
                        val progress = objects[0] as TranslationProgress;
                        progressIndicator.fraction = progress.currentTask.toDouble() / max(progress.taskAmount.toDouble(), 1.0)
                        progressIndicator.text2 = "${progress.currentTask}/${progress.taskAmount}"
                    } else {
                        throw IllegalStateException("Unknown message received");
                    }
                }
                con.subscribe(TranslationProgressChangeNotifier.CHANGE_ACTION_TOPIC)
            }
        }

        progressManager.runProcessWithProgressAsynchronously(myTask,
            object : BackgroundableProcessIndicator(myTask) {}
        );
    }
}
