package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.dto.BestGuessWithPsiReference
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.border.LineBorder
import javax.swing.event.DocumentEvent

/**
 * A dialog for users to verify and adapt l10n entry guesses.
 * Refactored to use the modern IntelliJ UI DSL for a cleaner look and more maintainable code.
 */
class BestGuessAdaptionDialog(
    bestGuessResponse: List<BestGuessWithPsiReference>,
    availableLanguages: List<Language>,
) : DialogWrapper(true) {
    // --- UI Components ---
    private lateinit var contextTextArea: JBTextArea
    private lateinit var elementDesiredKeyTextArea: JBTextField
    private lateinit var elementDesiredValueTextArea: JBTextField
    private lateinit var elementDesiredDescriptionTextArea: JBTextField
    private lateinit var elementPlaceholderTextArea: JBTextArea
    private lateinit var placeholderRow: Row
    private var psiCheckboxes: List<JBCheckBox> = emptyList()

    // --- Data Models ---
    private var currentBestGuessWithPsiReferenceModel: BestGuessWithPsiReferenceModel? = null
    private val languageCheckboxModels: List<LanguageCheckboxModel>
    private val bestGuessWithPsiReferenceModel: List<BestGuessWithPsiReferenceModel>

    init {
        title = "Adapt Best Guesses"
        setOKButtonText("Translate All Keys")
        setCancelButtonText("Cancel")

        languageCheckboxModels = availableLanguages.map { LanguageCheckboxModel(true, it) }

        bestGuessWithPsiReferenceModel = bestGuessResponse.map {
            BestGuessWithPsiReferenceModel(
                checked = true,
                literalReference = it,
                adaptedBestGuess = AdaptedBestGuess(it.bestGuess.id, it.bestGuess.key, it.psiElement.text.removeSurrounding("\""), it.bestGuess.description, it.bestGuess.placeholder)
            )
        }

        // The first model is selected by default.
        currentBestGuessWithPsiReferenceModel = bestGuessWithPsiReferenceModel.firstOrNull()

        // Initializes the dialog and builds the UI by calling createCenterPanel()
        super.init()
    }

    /**
     * Creates the main content of the dialog.
     * The layout is a main horizontal splitter, with the list of entries on the left
     * and the details view on the right.
     */
    override fun createCenterPanel(): JComponent {
        val leftPanel = createEntriesListPanel() // The left side with languages and checkboxes
        val rightPanel = createDetailsPanel()    // The right side with context and edit fields

        // When the dialog opens, populate the right panel with the details of the first item.
        bestGuessWithPsiReferenceModel.firstOrNull()?.let { updateDetailsPanel(it) }

        return JBSplitter(false, 0.4f).apply {
            firstComponent = leftPanel
            secondComponent = rightPanel
            preferredSize = Dimension(850, 600)
        }
    }

    //================================================================================
    // Left Panel - Entry List
    //================================================================================

    /**
     * Creates the entire left panel, which includes the language selection and the
     * scrollable list of guessed entries.
     */
    private fun createEntriesListPanel(): JComponent {
        return JPanel(BorderLayout()).apply {
            add(createLanguagesPanel(), BorderLayout.NORTH)
            add(createPsiElementCheckboxList(), BorderLayout.CENTER)
        }
    }

    /**
     * Creates the language selection panel at the top-left using the UI DSL.
     */
    private fun createLanguagesPanel(): DialogPanel {
        return panel {
            group("Translate to Languages") {
                row {
                    languageCheckboxModels.forEach { model ->
                        checkBox(model.language.toISOLangString())
                            .bindSelected(model::checked)
                    }
                }
            }
        }
    }

    /**
     * Creates the scrollable list of checkboxes for each localization entry.
     * This uses a traditional BoxLayout for the vertical list.
     */
    private fun createPsiElementCheckboxList(): JComponent {
        val checkboxPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
        }

        val checkboxList = mutableListOf<JBCheckBox>()
        bestGuessWithPsiReferenceModel.forEachIndexed { index, model ->
            val cb = JBCheckBox(model.literalReference.psiElement.text, model.checked)
            cb.addActionListener { model.checked = cb.isSelected }
            cb.addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    updateDetailsPanel(model) // Update right panel on focus
                }
            })
            cb.addKeyListener(object : KeyAdapter() {
                override fun keyPressed(e: KeyEvent) {
                    when (e.keyCode) {
                        KeyEvent.VK_SPACE -> {
                            cb.isSelected = !cb.isSelected
                            model.checked = cb.isSelected
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
        psiCheckboxes = checkboxList

        return JBScrollPane(checkboxPanel).apply {
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }
    }


    //================================================================================
    // Right Panel - Details View
    //================================================================================

    /**
     * Creates the right-side details view, which itself is a vertical splitter
     * containing the context view and the editing form.
     */
    private fun createDetailsPanel(): JComponent {
        return JBSplitter(true, 0.5f).apply { // Vertical splitter
            firstComponent = createContextPanel()
            secondComponent = createTranslationPanel()
        }
    }

    /**
     * Creates the top-right panel to display the code context of the selected string.
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
                            isFocusable = false // Prevent tabbing into a read-only area
                            lineWrap = true
                            wrapStyleWord = true
                        }
                }.resizableRow()
            }
        }
    }

    /**
     * Creates the bottom-right panel with text fields for editing the key, value, and description.
     */
    private fun createTranslationPanel(): DialogPanel {
        val detailsPanel = panel {
            group("Localization Details") {
                row("Desired Key:") {
                    textField()
                        .align(AlignX.FILL)
                        .also { elementDesiredKeyTextArea = it.component }
                }
                row("Desired Value:") {
                    textField()
                        .align(AlignX.FILL)
                        .also { elementDesiredValueTextArea = it.component }
                }
                row("Description:") {
                    textField()
                        .align(AlignX.FILL)
                        .also { elementDesiredDescriptionTextArea = it.component }
                }
                placeholderRow = row("Placeholder (JSON):") {
                    textArea()
                        .align(Align.FILL)
                        .also { elementPlaceholderTextArea = it.component }
                        .applyToComponent {
                            lineWrap = true
                            wrapStyleWord = true
                        }
                }.resizableRow()
            }
        }

        // After creating the components, add listeners to write user changes back to our data model.
        val listener = object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                currentBestGuessWithPsiReferenceModel?.adaptedBestGuess?.let { guess ->
                    when (e.document) {
                        elementDesiredKeyTextArea.document -> guess.key = elementDesiredKeyTextArea.text
                        elementDesiredValueTextArea.document -> guess.desiredValue = elementDesiredValueTextArea.text
                        elementDesiredDescriptionTextArea.document -> guess.description = elementDesiredDescriptionTextArea.text
                    }
                }
            }
        }
        elementDesiredKeyTextArea.document.addDocumentListener(listener)
        elementDesiredValueTextArea.document.addDocumentListener(listener)
        elementDesiredDescriptionTextArea.document.addDocumentListener(listener)

        val objectMapper = ObjectMapper()
        elementPlaceholderTextArea.document.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                val text = elementPlaceholderTextArea.text
                try {
                    val placeholderMap: Map<String, Any?> = objectMapper.readValue(text)
                    currentBestGuessWithPsiReferenceModel?.adaptedBestGuess?.placeholder = placeholderMap
                    elementPlaceholderTextArea.border = null // Reset border on success
                } catch (ex: Exception) {
                    // Invalid JSON, set red border
                    elementPlaceholderTextArea.border = LineBorder(com.intellij.ui.JBColor.RED)
                }
            }
        })

        return detailsPanel
    }

    //================================================================================
    // Controller Logic & Data Handling
    //================================================================================

    /**
     * This is the "controller" method. It's called when focus changes on the left-side
     * list and is responsible for populating the right-side panels with the correct data.
     */
    private fun updateDetailsPanel(model: BestGuessWithPsiReferenceModel) {
        currentBestGuessWithPsiReferenceModel = model

        // 1. Update the context text area
        val contextFinder = FlutterArbTranslationInitializer().literalInContextFinder
        contextTextArea.text = contextFinder.findContext(model.literalReference.psiElement).text

        // 2. Update the editable text fields
        // We set the text directly instead of using bindText, because the underlying data *object*
        // changes with each selection. The DocumentListeners we added handle the reverse flow (UI -> model).
        elementDesiredKeyTextArea.text = model.adaptedBestGuess.key
        elementDesiredValueTextArea.text = model.adaptedBestGuess.desiredValue
        elementDesiredDescriptionTextArea.text = model.adaptedBestGuess.description

        // 3. Update placeholder text area
        val placeholder = model.adaptedBestGuess.placeholder
        if (placeholder != null && placeholder.isNotEmpty()) {
            placeholderRow.visible(true)
            val objectMapper = ObjectMapper()
            elementPlaceholderTextArea.text = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(placeholder)
        } else {
            placeholderRow.visible(false)
            elementPlaceholderTextArea.text = ""
        }
    }

    /**
     * Sets the initial focus on the first checkbox in the list.
     */
    override fun getPreferredFocusedComponent(): JComponent? {
        return psiCheckboxes.firstOrNull()
    }

    /**
     * Collects the final, user-modified data from the models when the OK button is clicked.
     */
    fun getUserInput(): UserAdaptedGuesses {
        return UserAdaptedGuesses(
            selectedLanguages = languageCheckboxModels.filter { it.checked }.map { it.language },
            adaptedGuesses = bestGuessWithPsiReferenceModel.filter { it.checked }.map { it.adaptedBestGuess }
        )
    }

    //================================================================================
    // Data Classes
    //================================================================================

    data class UserAdaptedGuesses(
        val selectedLanguages: List<Language>,
        val adaptedGuesses: List<AdaptedBestGuess>,
    )

    internal data class LanguageCheckboxModel(
        var checked: Boolean,
        val language: Language,
    )

    internal data class BestGuessWithPsiReferenceModel(
        var checked: Boolean,
        val literalReference: BestGuessWithPsiReference,
        var adaptedBestGuess: AdaptedBestGuess,
    )

    data class AdaptedBestGuess(
        val id: String,
        var key: String,
        var desiredValue: String,
        var description: String,
        var placeholder: Map<String, *>?
    )
}