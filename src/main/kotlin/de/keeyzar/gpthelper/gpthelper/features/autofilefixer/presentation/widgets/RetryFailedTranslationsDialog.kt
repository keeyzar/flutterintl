package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationRequest
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

class RetryFailedTranslationsDialog(
    private val failedRequests: List<UserTranslationRequest>
) : DialogWrapper(true) {

    private val models: List<FailedRequestModel>

    init {
        title = "Retry Failed Translations"
        setOKButtonText("Retry Selected")
        setCancelButtonText("Skip")
        models = failedRequests.map { FailedRequestModel(it) }
        super.init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(600, 400)

        val checkboxPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        models.forEach { model ->
            val text = "Key: ${model.request.baseTranslation.entry.desiredKey} - Value: ${model.request.baseTranslation.entry.desiredValue}"
            val cb = JBCheckBox(text, true)
            cb.addActionListener {
                model.checked = cb.isSelected
            }
            checkboxPanel.add(cb)
        }

        panel.add(JBScrollPane(checkboxPanel).apply {
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }, BorderLayout.CENTER)

        return panel
    }

    fun getRequestsToRetry(): List<UserTranslationRequest> {
        return models.filter { it.checked }.map { it.request }
    }

    internal data class FailedRequestModel(
        val request: UserTranslationRequest,
        var checked: Boolean = true
    )
}

