package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.application.ApplicationManager

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.ui.TerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.util.*
import kotlin.concurrent.schedule

class IdeaTerminalConsoleService {

    companion object {
        const val TAB_NAME_FOR_L10N_GENERATION = "L10N Generation"
    }

    fun executeCommand(project: Project, command: String) {
        ApplicationManager.getApplication().invokeLater {
            val terminalView = TerminalToolWindowManager.getInstance(project)
            val window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
                ?: return@invokeLater

            val contentManager = window.contentManager

            var content = contentManager.findContent(TAB_NAME_FOR_L10N_GENERATION)
            val widget: TerminalWidget

            if (content != null) {
                println("reusing terminal widget")
                widget = TerminalToolWindowManager.findWidgetByContent(content)!!
            } else {
                println("creating new terminal widget")
                widget = terminalView.createShellWidget(project.basePath, TAB_NAME_FOR_L10N_GENERATION, true, true)
                content = contentManager.factory.createContent(widget.component, TAB_NAME_FOR_L10N_GENERATION, false)
                contentManager.addContent(content)
            }

            window.activate(null)
            contentManager.setSelectedContent(content)
            widget.sendCommandToExecute(command)

            // After executing the command, schedule a VFS refresh.
            // This is a bit of a workaround, because we don't know when the command finishes.
            // A small delay should be sufficient for gen-l10n to complete.
            Timer().schedule(5000) {
                ApplicationManager.getApplication().invokeLater {
                    VirtualFileManager.getInstance().asyncRefresh(null)
                }
            }
        }
    }
}