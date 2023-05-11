package de.keeyzar.gpthelper.gpthelper.features.filetranslation.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.ui.dsl.builder.panel
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.model.FileContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.util.*

class TranslateWholeFileAction : DumbAwareAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val selectedFile = event.getData(PlatformDataKeys.VIRTUAL_FILE)
        if (selectedFile != null && !selectedFile.isDirectory && selectedFile.extension == "arb") {
            val processId = UUID.randomUUID();

            val flutterArbTranslationInitializer = FlutterArbTranslationInitializer()
            val contextProvider = flutterArbTranslationInitializer.contextProvider
            val translationTaskBackgroundProgress = flutterArbTranslationInitializer.translationTaskBackgroundProgress
            val fileTranslationProcessController = flutterArbTranslationInitializer.fileTranslationProcessController

            contextProvider.putContext(
                processId, FileContext(
                    baseFile = event.getData(PlatformDataKeys.PSI_FILE)!!,
                    project = event.project!!
                )
            )

            translationTaskBackgroundProgress.triggerInBlockingContext(event.project!!, {
                fileTranslationProcessController.startTranslationProcess(processId)
            }, {
                contextProvider.removeContext(processId)
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
