package de.keeyzar.gpthelper.gpthelper.features.review.infrastructure.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.ui.dsl.builder.panel
import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.OpenPageService
import java.awt.Desktop
import java.net.URI

class IdeaOpenPageService : OpenPageService {
    override fun openPage(link: URI) {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            val desktop = Desktop.getDesktop()
            desktop.browse(link)
        } else {
            showErrorDialog(link)
        }
    }

    private fun showErrorDialog(link: URI) {
        ApplicationManager.getApplication().invokeAndWait {
            val builder = DialogBuilder()
            builder.setTitle("Could Not Open Review Page...")
            builder.addOkAction()
            builder.setCenterPanel(panel {
                row {
                    label(
                        "Could not open review page, please open it manually:<br/>" +
                                "<a href=\"${link}\">${link}</a>"
                    )
                }
            })
            builder.show()
        }
    }
}
