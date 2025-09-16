package de.keeyzar.gpthelper.gpthelper.features.filetranslation.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.ui.dsl.builder.panel
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.actions.ProjectAwareAction
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.model.TranslateWholeFileContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.util.*

class TranslateWholeFileAction : ProjectAwareAction() {
    override fun actionPerformed(e: AnActionEvent, project: Project, initializer: FlutterArbTranslationInitializer) {
        val selectedFile = e.getData(PlatformDataKeys.VIRTUAL_FILE)
        if (selectedFile != null && !selectedFile.isDirectory && selectedFile.extension == "arb") {
            val processId = UUID.randomUUID();

            val contextProvider = initializer.contextProvider
            val translationTaskBackgroundProgress = initializer.translationTaskBackgroundProgress
            val fileTranslationProcessController = initializer.fileTranslationProcessController

            contextProvider.putTranslateWholeFileContext(
                processId, TranslateWholeFileContext(
                    baseFile = e.getData(PlatformDataKeys.PSI_FILE)!!,
                    project = project
                )
            )

            val uuid = UUID.randomUUID().toString()
            val translationContext = TranslationContext(uuid, "Translation Init", 0, null, 0)
            translationTaskBackgroundProgress.triggerInBlockingContext(project, {
                fileTranslationProcessController.startTranslationProcess(translationContext, processId)
            }, translationContext = translationContext, {
                contextProvider.removeWholeFileContext(processId)
            })

        } else {
            DialogBuilder().apply {
                setTitle("Wrong File Selected")
                addOkAction()
                setCenterPanel(panel {
                    row {
                        label("Please select an .arb file, which should be the base file for the translation")
                    }
                })
            }.show()
        }
    }
}
