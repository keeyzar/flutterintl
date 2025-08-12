package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowManager
import org.jetbrains.plugins.terminal.ShellTerminalWidget
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.util.*
import kotlin.concurrent.schedule

/**
 * this is somehow a little bit buggy...
 */
class IdeaTerminalConsoleService {

    companion object {
        const val TAB_NAME_FOR_L10N_GENERATION = "L10N Generation"
    }

    fun executeCommand(project: Project, command: String) {
        ApplicationManager.getApplication().invokeLaterOnWriteThread {
            val terminalView = TerminalToolWindowManager.getInstance(project)
            val window = ToolWindowManager.getInstance(project).getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
            val contentManager = window?.contentManager

            val widget = when (contentManager?.findContent(TAB_NAME_FOR_L10N_GENERATION)) {
                null -> {
                    println("creating new terminal widget")
                    WriteAction.compute<ShellTerminalWidget, Throwable> {
                        terminalView.createLocalShellWidget(project.basePath, TAB_NAME_FOR_L10N_GENERATION)
                    }
                } //don't open a new one all the time, is hella expensive
                else -> {
                    println("reusing terminal widget")
                    TerminalToolWindowManager.getWidgetByContent(contentManager.findContent(TAB_NAME_FOR_L10N_GENERATION)) as ShellTerminalWidget
                }
            }

            widget.executeCommand(command)

            // After executing the command, schedule a VFS refresh.
            // This is a bit of a workaround, because we don't know when the command finishes.
            // A small delay should be sufficient for gen-l10n to complete.
            Timer().schedule(2000) {
                ApplicationManager.getApplication().invokeLater {
                    VirtualFileManager.getInstance().asyncRefresh(null)
                }
            }
        }
    }
}
