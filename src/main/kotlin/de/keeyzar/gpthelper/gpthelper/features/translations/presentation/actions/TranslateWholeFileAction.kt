package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.VirtualFile
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.Initializer
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.widgets.GeneralInputRequestDialog
import javax.swing.JPanel

class TranslateWholeFileAction : DumbAwareAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val selectedFile = event.getData(PlatformDataKeys.VIRTUAL_FILE)
        if (selectedFile != null && !selectedFile.isDirectory && selectedFile.extension == "arb") {
            translateBasedOnUserInput(selectedFile, event)
        } else {
            val builder = DialogBuilder()
            builder.setCenterPanel(JPanel().apply {
                add(
                    javax.swing.JLabel(
                        "Please select a file with extension arb"
                    )
                )
            }.also { builder.setCenterPanel(it) })

            builder.show()
        }
    }

    private fun translateBasedOnUserInput(selectedFile: VirtualFile, event: AnActionEvent) {
        //show dialog to get target Language
        val dialog = GeneralInputRequestDialog(
            title = "Translate whole file",
            label = "Please enter the target language in format en_US or de",
        )
        val closedWithOk = dialog.showAndGet()
        if (closedWithOk) {
            translateFile(selectedFile, dialog, event)
        }
    }

    private fun translateFile(
        selectedFile: VirtualFile,
        dialog: GeneralInputRequestDialog,
        event: AnActionEvent
    ) {
        val inputStream = selectedFile.inputStream
        val content = inputStream.bufferedReader().use { it.readText() }
        val targetLanguage = dialog.userInput
        val psiDirectory = event.getData(PlatformDataKeys.PSI_FILE)!!.parent!!
        val wholeFileTranslationService = Initializer().wholeFileTranslationService
        wholeFileTranslationService.translateInBackground(
            psiDirectory,
            content,
            targetLanguage
        )
    }
}
