package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserAddNewLanguageRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.TranslationService
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.TranslationProgressChangeNotifier
import kotlinx.coroutines.runBlocking
import kotlin.math.max

/**
 * TODO this one is important, because I still need that haha.
 */
class WholeFileTranslationService(
    private val translationService: TranslationService,
) {
    /**
     * @param psiDirectory the element, that triggered the translation
     * @param userTranslateStringRequest the user request, that triggered the translation
     * @param dummyTranslationsToReplace the dummy translations, that should be replaced. It was generated, so that the UI is not blocked
     */
    fun translateInBackground(
        psiDirectory: PsiDirectory,
        content: String,
        targetLanguage: String,
    ) {
        val progressManager = ProgressManager.getInstance()
        val addNewLanguageRequest = UserAddNewLanguageRequest(content, targetLanguage);

        val myTask = object : Task.Backgroundable(psiDirectory.project, "Translating file", false) {
            var newFileContent: String? = null;
            override fun run(visibleIndicator: ProgressIndicator) {
                initProgressListener(visibleIndicator)
                newFileContent = runBlocking {
                    translationService.translateWholeFile(addNewLanguageRequest);
                }
            }

            override fun onSuccess() {
                super.onSuccess()
                ApplicationManager.getApplication().runWriteAction() {
                    newFileContent?.let { newContent ->
                        val directoryPath: String = psiDirectory.virtualFile.path // get the directory path
                        val fileName = "app_$targetLanguage.arb" // specify the name of the new file
                        val file: VirtualFile = LocalFileSystem.getInstance().findFileByPath("$directoryPath/$fileName") ?: run {
                            psiDirectory.createFile(fileName).virtualFile
                        }

                        val psiFile: PsiFile? = PsiManager.getInstance(project).findFile(file)

                        psiFile?.let {
                            val doc = PsiDocumentManager.getInstance(project).getDocument(psiFile)
                            doc?.setText(newContent)
                        }
                    }
                }
            }

            fun initProgressListener(progressIndicator: ProgressIndicator) {
                progressIndicator.fraction = 0.0;
                progressIndicator.isIndeterminate = false;
                val con = psiDirectory.project.messageBus.connect();
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
