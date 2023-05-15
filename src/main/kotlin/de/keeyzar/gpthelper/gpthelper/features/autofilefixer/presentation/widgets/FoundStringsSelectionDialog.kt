package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets

import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.PsiElement
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.actionListener
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.dsl.gridLayout.VerticalAlign
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.awt.Dimension
import java.awt.KeyboardFocusManager
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.*

class FoundStringsSelectionDialog(
    private val allTextStrings: Set<PsiElement>,
) : DialogWrapper(true) {

    private lateinit var textArea: Cell<JBTextArea>
    private var allCheckboxes: List<Cell<JBCheckBox>> = ArrayList()
    private val internalModel: List<PsiToModelElement> = allTextStrings.map {
        PsiToModelElement(true, it)
    }

    init {
        title = "Select Strings to Localize"
        setOKButtonText("Localize")
        init()
    }

    override fun createCenterPanel(): JComponent {
        val leftPanel = createCheckboxList()
        val rightPanel = createContextView()

        configureTraversal(leftPanel, rightPanel)

        val splitPane = JBSplitter(false)
        splitPane.firstComponent = leftPanel
        splitPane.secondComponent = rightPanel
        splitPane.proportion = 0.5f
        splitPane.preferredSize = Dimension(800, 400)

        return splitPane
    }

    private fun createCheckboxList(): DialogPanel {
        val leftPanel = panel {
            internalModel.map {
                //now we have all checkboxes
                row {
                    allCheckboxes += checkBox(it.psiElement.text)
                        .actionListener { _, component ->
                            it.isSelected = component.isSelected
                        }
                        .apply { component.isSelected = it.isSelected }
                        .apply {
                            this.component.addFocusListener(object : FocusListener {
                                override fun focusGained(e: FocusEvent?) {
                                    //show a little bit of context, might be improved in the future, for example with a dummy preview below
                                    textArea.component.text = FlutterArbTranslationInitializer().literalInContextFinder.findContext(it.psiElement).text
                                }

                                override fun focusLost(e: FocusEvent?) {
                                    //ignore
                                }
                            })
                        }

                }
            }
        }
        return leftPanel
    }

    private fun createContextView(): DialogPanel {
        val rightPanel = panel {
            //scrollable textarea
            row {
                textArea = textArea()
                    .resizableColumn()
                    .horizontalAlign(HorizontalAlign.FILL)
                    .verticalAlign(VerticalAlign.FILL)
                    .apply {
                        component.isEditable = false
                    }
            }
                .resizableRow()
        }
        return rightPanel
    }

    private fun configureTraversal(leftPanel: DialogPanel, rightPanel: DialogPanel) {
        //only checkbox traversal, also with down key it's easier
        leftPanel
            .setFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                setOf(KeyStroke.getKeyStroke("DOWN"), KeyStroke.getKeyStroke("TAB"))
            )
        //also register up key and shift tab
        leftPanel
            .setFocusTraversalKeys(
                KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
                setOf(KeyStroke.getKeyStroke("UP"), KeyStroke.getKeyStroke("shift TAB"))
            )
        rightPanel
            .setFocusTraversalKeys(
                KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
                setOf()
            )
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return allCheckboxes.first().component
    }

    fun getSelectedElements(): List<PsiElement> {
        return internalModel
            .filter { it.isSelected }
            .map { it.psiElement }
    }

    override fun doOKAction() {
        if (internalModel.any { it.isSelected }) {
            super.doOKAction()
        } else {
            //TODO should use intellij error dialog I guess
            JOptionPane.showMessageDialog(
                null,
                "Please select at least one string to localize.",
                "No Strings Selected",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    data class PsiToModelElement(var isSelected: Boolean, val psiElement: PsiElement)
}
