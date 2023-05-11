package de.keeyzar.gpthelper.gpthelper.features.filetranslation.presentation.widgets

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

class TargetLanguageDialog : DialogWrapper(true) {
    private lateinit var textField: Cell<JBTextField>
    private var model: Model = Model("")
    private val regex = """^[a-z]{2}(?:_[A-Z]{2})?$""".toRegex()
    init {
        title = "Enter Target Language"
        setOKButtonText("Translate")
        setCancelButtonText("Cancel")
        init()
    }

    override fun createCenterPanel(): JComponent {
        model = Model("")
        val panel: JPanel = panel {
            row {
                label("Please enter the target language, like en_US or en")
                textField = textField()
                    .bindText(model::targetLanguage)
                    .validation {
                        if (!regex.containsMatchIn(it.text)) {
                            error("is not a valid input, please use the format en_US or en")
                        } else {
                            null
                        }
                    }
            }
        }
        panel.minimumSize = Dimension(200, 50)
        return panel
    }

    fun getTargetLanguage(): String {
        return model.targetLanguage
    }

    internal data class Model(
        var targetLanguage: String
    )

}
