package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.GridBag
import com.intellij.util.ui.JBUI
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.dto.BestGuessWithPsiReference
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.awt.Dimension
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.KeyboardFocusManager
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*

/**
 * we have requests guesses for l10n entries, the user needs to verify and adapt these
 */
class BestGuessAdaptionDialog(
    bestGuessResponse: List<BestGuessWithPsiReference>,
    availableLanguages: List<Language>,
) : DialogWrapper(true) {
    private lateinit var contextTextArea: JBTextArea
    private lateinit var elementDesiredKeyTextArea: JBTextField
    private lateinit var elementDesiredValueTextArea: JBTextField
    private lateinit var elementDesiredDescriptionTextArea: JBTextField
    private var bestGuessSelectionListener: ((BestGuessWithPsiReferenceModel) -> Unit)? = null
    private var currentBestGuessWithPsiReferenceModel: BestGuessWithPsiReferenceModel? = null
    private var languageCheckboxModels: List<LanguageCheckboxModel>
    private var bestGuessWithPsiReferenceModel: List<BestGuessWithPsiReferenceModel>
    private var psiCheckboxes: List<JBCheckBox> = mutableListOf()

    init {
        title = "Adapt Best Guesses"
        setOKButtonText("Translate All Keys")
        setCancelButtonText("Cancel")
        languageCheckboxModels = availableLanguages.map {
            LanguageCheckboxModel(true, it)
        }.toMutableList()

        bestGuessWithPsiReferenceModel = bestGuessResponse.map {
            //initialize with best guess
            BestGuessWithPsiReferenceModel(true, it, AdaptedBestGuess(it.bestGuess.id, it.bestGuess.key, it.psiElement.text.removeSurrounding("\""), it.bestGuess.description))
        }
        bestGuessSelectionListener = listenToBestGuessSelectionChanges()
        super.init()
    }

    fun getUserInput(): UserAdaptedGuesses {
        return UserAdaptedGuesses(
            selectedLanguages = languageCheckboxModels.filter {
                it.checked
            }.map {
                it.language
            }.toList(),
            adaptedGuesses = bestGuessWithPsiReferenceModel
                .filter { it.checked }
                .map {
                    it.adaptedBestGuess
                }
        )
    }

    data class UserAdaptedGuesses(
        val selectedLanguages: List<Language>,
        val adaptedGuesses: List<AdaptedBestGuess>,
    )

    private fun listenToBestGuessSelectionChanges(): ((BestGuessWithPsiReferenceModel) -> Unit) {
        return {
            currentBestGuessWithPsiReferenceModel = it
            contextTextArea.text = FlutterArbTranslationInitializer().literalInContextFinder.findContext(it.literalReference.psiElement).text
            elementDesiredKeyTextArea.text = it.adaptedBestGuess.key
            elementDesiredValueTextArea.text = it.adaptedBestGuess.desiredValue
            elementDesiredDescriptionTextArea.text = it.adaptedBestGuess.description
        }
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val bag = GridBag()
            .setDefaultAnchor(GridBagConstraints.NORTHWEST)
            .setDefaultInsets(JBUI.insets(6))

        panel.add(
            createLanguagesPanel(), bag.nextLine().next()
                .insetRight(24)
        )
        panel.add(
            JSeparator(JSeparator.VERTICAL), bag.next()
                .fillCellVertically()
        )

        panel.add(createAllEntriesPanel(), bag.next()
            .anchor(GridBagConstraints.NORTH)
            .fillCell())
        return panel
    }

    private fun createLanguagesPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val bag = GridBag()
            .setDefaultAnchor(0, GridBagConstraints.NORTHWEST)
            .setDefaultFill(GridBagConstraints.HORIZONTAL)

        panel.add(JLabel("Select Languages For All Entries"), bag.nextLine().next())
        languageCheckboxModels.map {
            val checkBox = JBCheckBox(it.language.toISOLangString())
            checkBox.isSelected = it.checked
            checkBox.addActionListener { _ ->
                it.checked = checkBox.isSelected
            }
            panel.add(checkBox, bag.nextLine().next())
        }
        return panel
    }

    private fun createAllEntriesPanel(): JComponent {
        val leftPanel = createPsiElementCheckboxView()
        val rightPanel = createContextView()

        //TODO configure Traversal

        val splitPane = JBSplitter(false)
        splitPane.firstComponent = leftPanel
        splitPane.secondComponent = rightPanel
        splitPane.proportion = 0.3f
        splitPane.preferredSize = Dimension(600, 400)

        return splitPane
    }

    private fun createContextView(): JComponent {
        val splitPane = JBSplitter(true)
        splitPane.firstComponent = createContextPanel()
        splitPane.secondComponent = createTranslationPanel()
        splitPane.proportion = 0.5f
        splitPane.preferredSize = Dimension(400, 400)
        //TODO divider is not visible
        splitPane.dividerWidth = 2
        return splitPane
    }

    private fun createTranslationPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val bag = GridBag()
            .setDefaultAnchor(GridBagConstraints.NORTHWEST)
            .setDefaultWeightX(1, 5.0)
            .setDefaultFill(GridBagConstraints.HORIZONTAL)
            .setDefaultInsets(JBUI.insets(6))


        panel.add(JLabel("Desired Value"), bag.nextLine().next())

        elementDesiredValueTextArea = JBTextField()
        panel.add(elementDesiredValueTextArea, bag.next())

        panel.add(JLabel("Desired Key"), bag.nextLine().next())

        elementDesiredKeyTextArea = JBTextField()
        panel.add(elementDesiredKeyTextArea, bag.next())

        panel.add(JLabel("Desired Description"), bag.nextLine().next())

        elementDesiredDescriptionTextArea = JBTextField()
        panel.add(elementDesiredDescriptionTextArea, bag.next())

        panel.preferredSize = Dimension(400, 200)

        return panel
    }

    private fun createContextPanel(): JPanel {
        val panel = JPanel(GridBagLayout())
        val bag = GridBag()
            .setDefaultAnchor(1, GridBagConstraints.NORTHWEST)
            .setDefaultFill(GridBagConstraints.BOTH)
            .setDefaultInsets(JBUI.insets(6))

        //TODO make this text area fill the available space, rather than hardcoded rows and columns
        panel.add(JLabel("Where is this string from?"), bag.nextLine().next())
        contextTextArea = JBTextArea()
        contextTextArea.lineWrap = true
        contextTextArea.wrapStyleWord = true

        //make contextArea not focusable
        contextTextArea.addFocusListener(object : FocusAdapter() {
            override fun focusGained(e: FocusEvent?) {
                //based on cause forward focus to the next component
                when(e?.cause){
                    FocusEvent.Cause.TRAVERSAL_FORWARD -> {
                        e.component.transferFocus()
                    }
                    FocusEvent.Cause.TRAVERSAL_BACKWARD -> {
                        e.component.transferFocusBackward()
                    }
                    else -> {
                        //do nothing
                    }
                }
            }
        })

        panel.add(contextTextArea, bag.nextLine().next().fillCell().weightx(1.0).weighty(1.0))
        return panel
    }

    private fun createPsiElementCheckboxView(): JComponent {
        val panel = JPanel(GridBagLayout())
        val bag = GridBag()
            .setDefaultAnchor(GridBagConstraints.NORTHWEST)
            .setDefaultFill(GridBagConstraints.HORIZONTAL)
            .setDefaultInsets(JBUI.insets(6))

        bestGuessWithPsiReferenceModel.map {
            val checkBox = JBCheckBox(it.literalReference.psiElement.text)
            checkBox.isSelected = it.checked
            checkBox.addActionListener { _ ->
                it.checked = checkBox.isSelected
            }
            checkBox.addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent?) {
                    listenToBestGuessSelectionChanges().invoke(it)
                }
            })
            psiCheckboxes += checkBox
            panel.add(checkBox, bag.nextLine().next())
        }

        panel.add(JPanel(), bag.nextLine().next().fillCell().weightx(1.0).weighty(1.0))

        panel.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, setOf(KeyStroke.getKeyStroke("TAB"), KeyStroke.getKeyStroke("DOWN")))
        panel.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, setOf(KeyStroke.getKeyStroke("shift TAB"), KeyStroke.getKeyStroke("shift DOWN")))
        return panel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return psiCheckboxes[0]
    }

    internal data class LanguageCheckboxModel(
        var checked: Boolean,
        val language: Language,
    )

    internal data class BestGuessWithPsiReferenceModel(
        var checked: Boolean,
        val literalReference: BestGuessWithPsiReference,
        var adaptedBestGuess: AdaptedBestGuess,
    ) {
        fun reset() {
            this.adaptedBestGuess = AdaptedBestGuess(literalReference.id, literalReference.bestGuess.key, literalReference.psiElement.text.removeSurrounding("\""), literalReference.bestGuess.description)
        }
    }

    data class AdaptedBestGuess(
        val id: String,
        var key: String,
        var desiredValue: String,
        var description: String,
    )
}
