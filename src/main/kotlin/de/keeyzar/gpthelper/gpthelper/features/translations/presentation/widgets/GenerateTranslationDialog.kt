package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.widgets

import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.KeyStrokeAdapter.getKeyStroke
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.panels.VerticalBox
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.pojo.TranslationDialogUserInput
import java.awt.event.ActionEvent
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JScrollPane


//show a plugin dialog, which shows the current selected text,
class GenerateTranslationDialog(
    private val translateKeyContext: TranslateKeyContext,
) : DialogWrapper(true) {
    private lateinit var desiredValue: Cell<JBTextField>
    private lateinit var desiredKey: Cell<JBTextField>
    private lateinit var desiredDescription: Cell<JBTextField>
    private lateinit var historyCombobox: Cell<ComboBox<String>>
    private val pattern = Regex("^[a-z][a-z_]*$")
    private var isValid = false

    /**
     * used as a pointer as to what the user currently wants to translate
     */
    private var currentLastUserInputIndex = 0


    private lateinit var _centerPanel: DialogPanel
    private var model = Model()

    init {
        title = "Add Translations for Selected Text"
        setOKButtonText("Translate")
        fillModelData()
        init() //creates center panel
        isKeyValid()
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return desiredValue.component;
    }

    private fun fillModelData() {
        model.desiredValue = translateKeyContext.statement;
        //most of the time the user is going to translate in the same file one after another, and they will have mostly identical structure
        model.desiredKey = translateToTranslationKey(translateKeyContext.statement);
        val directLastUserInput = translateKeyContext.lastUserInput?.getOrNull(0)
        model.desiredKey = directLastUserInput?.desiredKey ?: model.desiredKey
        model.desiredDescription = calculateDesiredDescription(translateKeyContext)
        model.desiredDescription = directLastUserInput?.desiredDescription ?: model.desiredDescription


        (directLastUserInput?.languagesToTranslate?.toMutableMap()
            ?: translateKeyContext.availableLanguages.associate { it.toISOLangString() to true }.toMutableMap())
            .forEach { (key, value) -> model.translationsChecked[key] = value }
    }

    private fun calculateDesiredDescription(translateKeyContext: TranslateKeyContext): String {
        //in the future we could theoretically ask chatgpt for a fitting key, based on the old keys, and where the file is
        return ""
    }

    /**
     * sets model.desiredKey and model.desiredDescription to the next pointer, starting with 0, when there is none
     */
    private fun traversalLastInput(next: Boolean) {
        val lenOfInputHistory = translateKeyContext.lastUserInput?.size ?: 0
        if (lenOfInputHistory > 0) {
            currentLastUserInputIndex = if (next) {
                (currentLastUserInputIndex + 1) % lenOfInputHistory
            } else {
                (currentLastUserInputIndex - 1) % lenOfInputHistory
            }
            //ensure that it is at least 0 or at most lenOfInputHistory - 1
            currentLastUserInputIndex = if (currentLastUserInputIndex < 0) {
                lenOfInputHistory - 1
            } else {
                currentLastUserInputIndex
            }
            val lastUserInput = translateKeyContext.lastUserInput?.getOrNull(currentLastUserInputIndex)
            model.desiredKey = lastUserInput?.desiredKey ?: model.desiredKey
            model.desiredDescription = lastUserInput?.desiredDescription ?: model.desiredDescription
            desiredKey.component.text = model.desiredKey
            desiredDescription.component.text = model.desiredDescription
            historyCombobox.component.selectedIndex = currentLastUserInputIndex
        }
//        val lenOfInputHistory = translateKeyContext.lastUserInput?.size ?: 0
//        if (lenOfInputHistory > 0) {
//            currentLastUserInputIndex = (currentLastUserInputIndex + 1) % lenOfInputHistory
//            val lastUserInput = translateKeyContext.lastUserInput?.getOrNull(currentLastUserInputIndex)
//            model.desiredKey = lastUserInput?.desiredKey ?: model.desiredKey
//            model.desiredDescription = lastUserInput?.desiredDescription ?: model.desiredDescription
//        }
    }


    private fun selectLastInput(lastDesiredKey: String) {
        val lastUserInput = translateKeyContext.lastUserInput?.find { it.desiredKey == lastDesiredKey }
        model.desiredKey = lastUserInput?.desiredKey ?: model.desiredKey
        model.desiredDescription = lastUserInput?.desiredDescription ?: model.desiredDescription
        desiredKey.component.text = model.desiredKey
        desiredDescription.component.text = model.desiredDescription
    }


    private fun isKeyValid(): Boolean {
        isValid = pattern.find(model.desiredKey) != null
        return isValid
    }

    override fun createCenterPanel(): JComponent {
        _centerPanel = panel {
            group {
                row {
                    label("History:")
                    historyCombobox = comboBox(historyToCombobox())
                        .whenItemSelectedFromUi {
                            selectLastInput(it)
                        }
                        .comment("The history of your last translations, helpful, when you want to maintain structure for keys without closing the window. You can also traverse with alt up / down.")
                    button("Next") {
                        traversalLastInput(true)
                    }
                    button("Previous") {
                        traversalLastInput(false)
                    }
                }
                row {
                    label("To translate:")
                    desiredValue = textField()
                        .bindText(model::desiredValue)
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("The text you want to translate")
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Translation key:")
                    desiredKey = textField()
                        .bindText(model::desiredKey)
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .validation {
                            return@validation if (pattern.matches(it.text)) {
                                null
                            } else {
                                ValidationInfo("Begins with letter [a-z] and should contain only lowercase letters and underscores '_'")
                            }
                        }
                        .comment("The key is used to identify the translation in the code. It must start with a letter and can only contain letters and underscores")

                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Description:")
                    desiredDescription = textField()
                        .bindText(model::desiredDescription)
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("Description is optional, is only for the programmer / translator")
                }.layout(RowLayout.PARENT_GRID)
                separator("Languages to Translate")
                    .comment("Select the languages you want to translate to")
                row {
                    cell(createScrollPane())
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                }
            }
        }.withPreferredSize(500, 500)

        addTraverseInputHistoryListener(_centerPanel)
        return _centerPanel
    }

    private fun addTraverseInputHistoryListener(panel: DialogPanel) {
        // Create an action with a shortcut
        // Create an action with a shortcut

        // Create an action with a shortcut
        val altUpAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                // Whatever you want to do, e.g. modify the text field
                traversalLastInput(false)
            }
        }
        val altDownAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                // Whatever you want to do, e.g. modify the text field
                traversalLastInput(true)
            }
        }

        // Assign a shortcut to the action
        val keyStrokeAltUp = getKeyStroke("alt UP")
        val keyStrokeAltDown = getKeyStroke("alt DOWN")
        val inputMap = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val actionMap = panel.actionMap
        inputMap.put(keyStrokeAltUp, "altUpAction")
        inputMap.put(keyStrokeAltDown, "altDownAction")
        actionMap.put("altUpAction", altUpAction)
        actionMap.put("altDownAction", altDownAction)
    }

    private fun historyToCombobox(): List<String> {
        return translateKeyContext.lastUserInput?.map { it.desiredKey } ?: listOf()
    }

    private fun createScrollPane(): JScrollPane {
        //create checkboxes for each language
        val vbox = VerticalBox();


        model.translationsChecked.map { entry ->
            val checkBox = JBCheckBox(entry.key)
            checkBox.isSelected = entry.value
            checkBox.addActionListener {
                model.translationsChecked[entry.key] = checkBox.isSelected
            }
            vbox.add(checkBox)
        }

        return JBScrollPane(vbox)
    }

    private fun translateToTranslationKey(currentValue: String): String {
        return specialCharStripper(currentValue.lowercase(Locale.getDefault()).replace(" ", "_").take(30))
    }

    private fun specialCharStripper(currentValue: String): String {
        return currentValue.replace(Regex("[^a-z_]"), "")
    }

    fun getDialogInput(): TranslationDialogUserInput {
        return TranslationDialogUserInput(
            model.translationsChecked,
            model.desiredValue,
            model.desiredKey,
            model.desiredDescription,
        )
    }

    internal class Model {
        val translationsChecked: MutableMap<String, Boolean> = HashMap()
        var desiredValue: String = ""
        var desiredKey: String = ""
        var desiredDescription: String = ""
        var isValid = AtomicBoolean(false)
    }
}
