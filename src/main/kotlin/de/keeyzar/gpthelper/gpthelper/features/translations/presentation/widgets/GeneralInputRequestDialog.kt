package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.widgets

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.util.ui.JBUI
import javax.swing.*

/**
 * This should show some kind of
 */
//TODO should be obsolete in some time, because we don't have the different windows anymore
//definitely obsolete, because it's only used by the translateWholeFileAction, which is also obsolete
class GeneralInputRequestDialog(
    private val title: String,
    private val label: String,
) : DialogWrapper(true) {
    private var userInputField = JTextField()
    private var _userInput: String = ""
    val userInput: String
        get() = _userInput

    override fun createCenterPanel(): JComponent? {
        setTitle(title)

        // Create a vertical box to hold the components
        val box = Box.createVerticalBox()

        // Add a label
        val label = JLabel(label)
        label.border = JBUI.Borders.empty(5)
        box.add(label)

        // Add the text field
        userInputField.border = JBUI.Borders.empty(5)
        box.add(userInputField)

        return box
    }

    init {
        init()
    }

    override fun doOKAction() {
        _userInput = userInputField.text
        super.doOKAction()
    }

}
