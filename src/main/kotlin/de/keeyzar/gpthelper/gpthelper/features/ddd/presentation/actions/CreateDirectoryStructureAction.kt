package de.keeyzar.gpthelper.gpthelper.features.ddd.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.vfs.VirtualFile
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.Initializer


class CreateDirectoryStructureAction : DumbAwareAction() {

    companion object {
        val DEFAULT_PROJECT_KEY = "default"
    }
    override fun actionPerformed(event: AnActionEvent) {
        val selectedFile: VirtualFile? = event.getData(PlatformDataKeys.VIRTUAL_FILE)
        val createDirectoryTreeService = Initializer().createDirectoryStructureService

        if (selectedFile != null) {
            runWriteCommandAction(event.project) {
                val dirs = createDirectoryTreeService.createDirectoryTree(event.project?.name!!, selectedFile)
                if(dirs == null) {
                    createDirectoryTreeService.createDirectoryTree(DEFAULT_PROJECT_KEY, selectedFile)
                }

            }

        }

    }
}
