package de.keeyzar.gpthelper.gpthelper.features.ddd.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import de.keeyzar.gpthelper.gpthelper.features.ddd.presentation.actions.CreateDirectoryStructureAction.Companion.DEFAULT_PROJECT_KEY
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.Initializer
import javax.swing.BoxLayout
import javax.swing.ButtonGroup
import javax.swing.JPanel
import javax.swing.JRadioButton

class MarkDirectoryStructureAction : DumbAwareAction() {

    private var chosenOption: DialogOption? = null

    override fun actionPerformed(event: AnActionEvent) {
        val selectedFile = event.getData(PlatformDataKeys.VIRTUAL_FILE)
        if (selectedFile == null) {
            Messages.showErrorDialog("Please select a directory", "Error")
            return
        }
        val files = treeWalkerGetAllDirectories(selectedFile)
        val directoryTreeService = Initializer().saveDirectoryTreeService

        val dialog = dialogProjectSpecificOrDefault()
        if(dialog.showAndGet()){
            runWriteAction {
                when (chosenOption) {
                    DialogOption.PROJECT_SPECIFIC -> directoryTreeService.saveDirectoryTree(event.project?.name!!, files, selectedFile)
                    DialogOption.DEFAULT -> directoryTreeService.saveDirectoryTree(DEFAULT_PROJECT_KEY, files, selectedFile)
                    null -> return@runWriteAction
                }

            }
        }
    }

    private fun dialogProjectSpecificOrDefault(): DialogBuilder {
        val builder = DialogBuilder()
        builder.title("Save Settings")

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)

        val projectSpecificRadioButton = JRadioButton("Save for this project")
        val defaultRadioButton = JRadioButton("Save as default")
        val buttonGroup = ButtonGroup()
        buttonGroup.add(projectSpecificRadioButton)
        buttonGroup.add(defaultRadioButton)
        panel.add(projectSpecificRadioButton)
        panel.add(defaultRadioButton)

        builder.centerPanel(panel)
        builder.addOkAction()
        builder.addCancelAction()
        builder.resizable(true)

        builder.setOkOperation {
            chosenOption = if (projectSpecificRadioButton.isSelected) {
                DialogOption.PROJECT_SPECIFIC
            } else {
                DialogOption.DEFAULT
            }
            builder.dialogWrapper.close(DialogWrapper.OK_EXIT_CODE)
        }

        return builder
    }

    private fun treeWalkerGetAllDirectories(selectedFile: VirtualFile): List<VirtualFile> {
        val result = mutableListOf<VirtualFile>()
        val children = selectedFile.children
        for (child in children) {
            if (child.isDirectory) {
                result.add(child)
                result.addAll(treeWalkerGetAllDirectories(child))
            }
        }
        return result
    }
}
