package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.widgets

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.VerticalBox
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.pojo.TranslationDialogUserInput
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JScrollPane


//show a plugin dialog, which shows the current selected text,
class GenerateTranslationDialog(
    private val translateKeyContext: TranslateKeyContext,
) : DialogWrapper(true) {

    private val pattern = Regex("^[a-z][a-z_]*\$")
    private var isValid = false;


    private lateinit var _centerPanel: JPanel
    private var model = Model()

    init {
        title = "Add Translations for Selected Text"
        setOKButtonText("Translate")
        fillModelData()
        init() //creates center panel
        isKeyValid()
    }

    private fun fillModelData() {
        model.desiredValue = translateKeyContext.statement;
        //most of the time the user is going to translate in the same file one after another, and they will have mostly identical structure
        model.desiredKey = translateToTranslationKey(translateKeyContext.statement);
        model.desiredKey = translateKeyContext.lastUserInput?.desiredKey ?: model.desiredKey
        model.desiredDescription = calculateDesiredDescription(translateKeyContext)
        model.desiredDescription = translateKeyContext.lastUserInput?.desiredDescription ?: model.desiredDescription


        (translateKeyContext.lastUserInput?.languagesToTranslate?.toMutableMap()
            ?: translateKeyContext.availableLanguages.associate { it.toISOLangString() to true }.toMutableMap())
            .forEach { (key, value) -> model.translationsChecked[key] = value }
    }

    private fun calculateDesiredDescription(translateKeyContext: TranslateKeyContext): String {
        //in the future we could theoretically ask chatgpt for a fitting key, based on the old keys, and where the file is
        return ""
    }

    private fun isKeyValid(): Boolean {
        isValid = pattern.find(model.desiredKey) != null
        return isValid
    }

    override fun createCenterPanel(): JComponent {
        _centerPanel = panel {
            group {
                row {
                    label("To translate:")
                    textField()
                        .bindText(model::desiredValue)
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("The text you want to translate")
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Translation key:")
                    textField()
                        .bindText(model::desiredKey)
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("The key is used to identify the translation in the code. It must start with a letter and can only contain letters and underscores")

                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Description:")
                    textField()
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
        return _centerPanel
    }

    private fun createScrollPane(): JScrollPane {
        //create checkboxes for each language
        val vbox = VerticalBox();

        translateKeyContext.availableLanguages.map { lang ->
            val checkBox = JBCheckBox(lang.toISOLangString())
            checkBox.isSelected = true
            checkBox.addActionListener {
                model.translationsChecked[lang.toISOLangString()] = checkBox.isSelected
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
