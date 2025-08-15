package de.keeyzar.gpthelper.gpthelper.common.error

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.GptHelperSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.NoTranslationFilesException
import java.io.PrintWriter
import java.io.StringWriter

/**
 * A central error handler that can be used to display different error messages depending on the exception type.
 */
class GeneralErrorHandler {
    private val log: Logger = Logger.getInstance(GeneralErrorHandler::class.java)

    fun handleError(project: Project, throwable: Throwable) {
        log.error("An error occurred, which will be handled by the GeneralErrorHandler", throwable)
        ApplicationManager.getApplication().invokeLater {
            when {
                isOrHasCauseOfType(throwable, NoTranslationFilesException::class.java) -> handleNoTranslationFilesException(project, throwable)
                else -> handleGenericException(project, throwable)
            }
        }
    }

    private fun <T : Throwable> isOrHasCauseOfType(throwable: Throwable, type: Class<T>): Boolean {
        var current: Throwable? = throwable
        while (current != null) {
            if (type.isInstance(current)) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private fun handleNoTranslationFilesException(project: Project, exception: Throwable) {
        val result = Messages.showOkCancelDialog(
            project,
            "It looks like the settings for the ARB file are incorrect. Please check your settings.",
            "Incorrect ARB File Settings",
            "Go to Settings",
            "Close",
            null
        )

        if (result == Messages.OK) {
            ShowSettingsUtil.getInstance().showSettingsDialog(project, GptHelperSettings::class.java)
        }
    }

    private fun handleGenericException(project: Project, throwable: Throwable) {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        val stackTrace = sw.toString()
        val errorMessage = """
            An unexpected error occurred during background processing.
            
            Please report this issue.
            
            Stack trace:
            $stackTrace
        """.trimIndent()
        Messages.showErrorDialog(project, errorMessage, "Background Task Error")
    }
}
