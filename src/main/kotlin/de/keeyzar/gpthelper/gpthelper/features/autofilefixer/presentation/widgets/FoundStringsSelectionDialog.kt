package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiElement
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.*
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane

class FoundStringsSelectionDialog(
    private val project: Project?, // Pass the project for native dialogs
    private val allTextStrings: Set<PsiElement>,
) : DialogWrapper(true) {

    // --- UI Components ---
    private lateinit var contextTextArea: JBTextArea
    private var allCheckboxes: List<JBCheckBox> = emptyList()

    // --- Data Model ---
    private val internalModel: List<PsiToModelElement> = allTextStrings.map {
        PsiToModelElement(true, it)
    }

    init {
        title = "Select Strings to Localize"
        setOKButtonText("Localize")
        // This call builds the UI via createCenterPanel()
        init()
    }

    /**
     * Creates the main dialog content, a splitter with the selection list on the left
     * and the context view on the right.
     */
    override fun createCenterPanel(): JComponent {
        val leftPanel = createSelectionListPanel()
        val rightPanel = createContextPanel()

        // Populate the context view with the first item's details initially.
        internalModel.firstOrNull()?.let { updateContextArea(it) }

        return JBSplitter(false, 0.5f).apply {
            firstComponent = leftPanel
            secondComponent = rightPanel
            preferredSize = Dimension(800, 400)
        }
    }

    //================================================================================
    // Panel Creation
    //================================================================================

    /**
     * Creates the left-side panel containing the scrollable list of checkboxes.
     */
    private fun createSelectionListPanel(): JComponent {
        val checkboxPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val checkboxList = mutableListOf<JBCheckBox>()
        internalModel.forEachIndexed { index, model ->
            val cb = JBCheckBox(model.psiElement.text, model.isSelected)

            // Let the Look-and-Feel handle all styling (colors, borders, focus highlights).
            // This ensures the plugin respects the user's chosen IDE theme.

            cb.addActionListener { model.isSelected = cb.isSelected }

            cb.addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    updateContextArea(model)
                }
            })

            cb.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    when (e.keyCode) {
                        KeyEvent.VK_SPACE -> {
                            cb.isSelected = !cb.isSelected
                            model.isSelected = cb.isSelected
                            e.consume()
                        }
                        KeyEvent.VK_DOWN -> if (index + 1 < checkboxList.size) checkboxList[index + 1].requestFocusInWindow()
                        KeyEvent.VK_UP -> if (index - 1 >= 0) checkboxList[index - 1].requestFocusInWindow()
                    }
                }
            })
            checkboxPanel.add(cb)
            checkboxList.add(cb)
        }
        allCheckboxes = checkboxList

        return JBScrollPane(checkboxPanel).apply {
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }
    }

    /**
     * Creates the right-side panel for showing the string's context using the UI DSL.
     */
    private fun createContextPanel(): DialogPanel {
        return panel {
            group("Context") {
                row {
                    textArea()
                        .align(Align.FILL)
                        .also { contextTextArea = it.component }
                        .applyToComponent {
                            isEditable = false
                            isFocusable = false // Important for good keyboard navigation
                            lineWrap = true
                            wrapStyleWord = true
                        }
                }.resizableRow()
            }
        }
    }

    //================================================================================
    // UI Logic & Data Handling
    //================================================================================

    /**
     * Updates the context text area based on the currently focused checkbox.
     */
    private fun updateContextArea(model: PsiToModelElement) {
        val contextFinder = FlutterArbTranslationInitializer().literalInContextFinder
        contextTextArea.text = contextFinder.findContext(model.psiElement).text
    }

    /**
     * Sets initial focus on the first checkbox in the list. This triggers its
     * focus listener, which in turn populates the context view.
     */
    override fun getPreferredFocusedComponent(): JComponent? {
        return allCheckboxes.firstOrNull()
    }

    /**
     * Gathers the final list of selected strings.
     */
    fun getSelectedElements(): List<PsiElement> {
        return internalModel
            .filter { it.isSelected }
            .map { it.psiElement }
    }

    /**
     * Overrides the OK action to validate that at least one string is selected.
     * Uses the native IntelliJ error message dialog instead of JOptionPane.
     */
    override fun doOKAction() {
        if (internalModel.any { it.isSelected }) {
            super.doOKAction()
        } else {
            Messages.showErrorDialog(
                project,
                "Please select at least one string to localize.",
                "No Strings Selected"
            )
        }
    }

    /**
     * Internal data class to link a PsiElement with its selection state.
     */
    data class PsiToModelElement(var isSelected: Boolean, val psiElement: PsiElement)
}