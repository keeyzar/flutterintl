package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.terminal.ui.TerminalWidget
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.ConsoleService
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager

class IdeaTerminalConsoleService(
    private val project: Project) : ConsoleService {

    companion object {
        const val TAB_NAME_FOR_L10N_GENERATION = "L10N Generation"
        private val LOG = Logger.getInstance(IdeaTerminalConsoleService::class.java)
    }

    override fun executeCommand(command: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val terminalView = TerminalToolWindowManager.getInstance(project)
                val window = ToolWindowManager.getInstance(project)
                    .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)

                if (window == null) {
                    LOG.warn("Terminal tool window not found")
                    return@invokeLater
                }

                val contentManager = window.contentManager
                var content = contentManager.findContent(TAB_NAME_FOR_L10N_GENERATION)
                var widget: TerminalWidget? = null

                // Try to reuse existing content if the widget is still valid
                if (content != null) {
                    widget = TerminalToolWindowManager.findWidgetByContent(content)

                    // Check if widget exists - if null, the content is stale
                    if (widget == null) {
                        LOG.info("Existing terminal widget is null, removing old content")
                        contentManager.removeContent(content, true)
                        content = null
                    } else {
                        // Try to use the widget to verify it's not disposed
                        try {
                            widget.terminalTitle // Access any property to verify widget is alive
                            LOG.info("Reusing existing terminal widget")
                        } catch (e: Exception) {
                            LOG.info("Existing terminal widget is disposed or invalid, removing old content", e)
                            contentManager.removeContent(content, true)
                            content = null
                            widget = null
                        }
                    }
                }

                // Create new widget if needed
                if (widget == null) {
                    LOG.info("Creating new terminal widget")
                    widget = terminalView.createShellWidget(
                        project.basePath,
                        TAB_NAME_FOR_L10N_GENERATION,
                        true,
                        true
                    )
                    content = contentManager.factory.createContent(
                        widget.component,
                        TAB_NAME_FOR_L10N_GENERATION,
                        false
                    )
                    content.isCloseable = true
                    contentManager.addContent(content)
                }

                // Activate window and select content
                window.activate(null)
                contentManager.setSelectedContent(content)

                // Send command to execute
                widget.sendCommandToExecute(command)

                // Schedule VFS refresh after command execution
                // Use a single-shot timer that doesn't hold references
                val timer = java.util.Timer("L10N-VFS-Refresh", true)
                timer.schedule(object : java.util.TimerTask() {
                    override fun run() {
                        ApplicationManager.getApplication().invokeLater {
                            try {
                                VirtualFileManager.getInstance().asyncRefresh(null)
                                LOG.info("VFS refresh completed after L10N generation")
                            } catch (e: Exception) {
                                LOG.error("Error during VFS refresh", e)
                            }
                        }
                        timer.cancel() // Cancel timer after execution
                    }
                }, 5000L)

            } catch (e: Exception) {
                LOG.error("Error executing terminal command: $command", e)
            }
        }
    }
}