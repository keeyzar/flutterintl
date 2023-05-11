package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.ui.dsl.builder.panel

class IdeaTranslationErrorProcessHandlerImpl : TranslationErrorProcessHandler {
    override fun displayErrorToUser(e: Throwable) {
        ApplicationManager.getApplication().invokeAndWait {
            DialogBuilder().apply {
                setTitle("Error")
                setCenterPanel(
                    panel {
                        row {
                            label(e.message ?: "Unknown Error, please check logs")
                        }
                    }
                )
                addOkAction()
            }.showAndGet()
        }
    }
}
