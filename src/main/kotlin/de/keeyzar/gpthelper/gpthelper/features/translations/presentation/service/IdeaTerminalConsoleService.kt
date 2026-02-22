package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.wm.ToolWindowManager
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.ConsoleService
import org.jetbrains.plugins.terminal.TerminalToolWindowFactory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.util.Timer
import java.util.TimerTask

class IdeaTerminalConsoleService(
    private val project: Project
) : ConsoleService {

    companion object {
        const val TAB_NAME_FOR_L10N_GENERATION = "L10N Generation"
        private val LOG = Logger.getInstance(IdeaTerminalConsoleService::class.java)
        private const val NEW_SHELL_STARTUP_DELAY_MS = 2000L
        private const val VFS_REFRESH_DELAY_MS = 5000L
    }

    override fun executeCommand(command: String) {
        ApplicationManager.getApplication().invokeLater {
            try {
                val manager = TerminalToolWindowManager.getInstance(project)
                val window = ToolWindowManager.getInstance(project)
                    .getToolWindow(TerminalToolWindowFactory.TOOL_WINDOW_ID)
                    ?: run {
                        LOG.warn("Terminal tool window not found")
                        return@invokeLater
                    }

                val contentManager = window.contentManager
                val existingContent = contentManager.findContent(TAB_NAME_FOR_L10N_GENERATION)
                val existingWidget = existingContent?.let { TerminalToolWindowManager.findWidgetByContent(it) }

                val isNewWidget: Boolean
                if (existingWidget != null) {
                    // Reuse the existing, still-alive terminal tab
                    LOG.info("Reusing existing terminal widget")
                    isNewWidget = false
                    window.activate(null)
                    contentManager.setSelectedContent(existingContent)
                    sendCommandDelayed(existingWidget::sendCommandToExecute, command, 0L)
                } else {
                    // Remove stale content entry if present
                    if (existingContent != null) {
                        LOG.info("Removing stale terminal content")
                        contentManager.removeContent(existingContent, true)
                    }

                    // createShellWidget registers the new tab in the tool window automatically
                    LOG.info("Creating new terminal widget")
                    isNewWidget = true
                    val newWidget = manager.createShellWidget(
                        project.basePath,
                        TAB_NAME_FOR_L10N_GENERATION,
                        true,  // requestFocus
                        true   // deferSession
                    )

                    window.activate(null)
                    contentManager.findContent(TAB_NAME_FOR_L10N_GENERATION)
                        ?.let { contentManager.setSelectedContent(it) }

                    // Give the shell process time to start before sending the command
                    sendCommandDelayed(newWidget::sendCommandToExecute, command, NEW_SHELL_STARTUP_DELAY_MS)
                }

                // Refresh the VFS so generated files appear in the project tree
                scheduleVfsRefresh(if (isNewWidget) NEW_SHELL_STARTUP_DELAY_MS + VFS_REFRESH_DELAY_MS else VFS_REFRESH_DELAY_MS)

            } catch (e: Exception) {
                LOG.error("Error executing terminal command: $command", e)
            }
        }
    }

    private fun sendCommandDelayed(send: (String) -> Unit, command: String, delayMs: Long) {
        Timer("L10N-Command-Sender", true).schedule(object : TimerTask() {
            override fun run() {
                ApplicationManager.getApplication().invokeLater {
                    try {
                        send(command)
                        LOG.info("Command sent to terminal: $command")
                    } catch (e: Exception) {
                        LOG.error("Error sending command to terminal", e)
                    }
                }
                cancel()
            }
        }, delayMs)
    }

    private fun scheduleVfsRefresh(delayMs: Long) {
        Timer("L10N-VFS-Refresh", true).schedule(object : TimerTask() {
            override fun run() {
                ApplicationManager.getApplication().invokeLater {
                    try {
                        VirtualFileManager.getInstance().asyncRefresh(null)
                        LOG.info("VFS refresh completed after L10N generation")
                    } catch (e: Exception) {
                        LOG.error("Error during VFS refresh", e)
                    }
                }
                cancel()
            }
        }, delayMs)
    }
}