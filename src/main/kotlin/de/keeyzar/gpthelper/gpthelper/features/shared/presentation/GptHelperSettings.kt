package de.keeyzar.gpthelper.gpthelper.features.shared.presentation

import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ComponentPredicate
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository.FlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.UserSettingsCorruptException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.UserSettingsMissingException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.CurrentProjectProvider
import java.nio.file.Path
import javax.swing.JCheckBox
import javax.swing.JComponent

class GptHelperSettings : Configurable {
    private var panel: DialogPanel? = null
    private lateinit var openAIKeyField: Cell<JBPasswordField>
    private lateinit var intlConfigFile: Cell<TextFieldWithBrowseButton>
    private lateinit var watchIntlConfigFile: Cell<JCheckBox>
    private lateinit var arbDirectory: Cell<TextFieldWithBrowseButton>
    private lateinit var templateArbFile: Cell<TextFieldWithBrowseButton>
    private lateinit var outputLocalizationFile: Cell<JBTextField>
    private lateinit var outputClass: Cell<JBTextField>
    private lateinit var nullableGetter: Cell<JCheckBox>
    private lateinit var userSettingsRepository: UserSettingsRepository
    private lateinit var flutterIntlSettingsRepository: FlutterIntlSettingsRepository
    private lateinit var currentProjectProvider: CurrentProjectProvider
    private var corruptSettings = false;
    private var errorListener: ((Boolean) -> Unit)? = null
    private val model = Model()

    override fun getDisplayName(): String = "GPTHelper Settings"
    override fun createComponent(): JComponent {
        userSettingsRepository = Initializer().userSettingsRepository
        flutterIntlSettingsRepository = Initializer().flutterIntlSettingsRepository
        currentProjectProvider = Initializer().currentProjectProvider

        panel = panel {
            group("General") {
                row {
                    label("OpenAI API Password:")
                    openAIKeyField = cell(JBPasswordField())
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                    button("Test Password") {
                        testPassword(openAIKeyField.component.password)
                    }
                    button("Save New Password") {
                        savePassword(openAIKeyField.component.password)
                    }
                }.layout(RowLayout.PARENT_GRID)
            }
            group("Flutter Intl") {
                row {
                    label("Intl Config File:")
                    intlConfigFile = textFieldWithBrowseButton()
                        .bindText(model::intlConfigFile)
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                    button("Refresh Settings based on intl config file") {
                        loadIntlSettingsFromFile(intlConfigFile.component.text)
                    }
                }
                row {
                    watchIntlConfigFile = checkBox("Watch Intl Config File")
                        .bindSelected(model::watchIntlConfigFile)
                        .comment("If enabled, the settings will be refreshed automatically if the intl config file changes. (I.e. on each invoke of the plugin.")

                }
                separator()
                row {
                    label("Arb Directory:")
                    arbDirectory = textFieldWithBrowseButton()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .resizableColumn()
                        .bindText(model::arbDir)
                        .comment("The directory where the arb files are located")
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Template Arb File:")
                    templateArbFile = textFieldWithBrowseButton()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(model::templateArbFile)
                        .comment(
                            """
                            |The template arb file, i.e. which is the main arb file, most of the time it's app_en.arb.
                            | Based on the template arb file, the other file names will be generated.
                            | Missing translations are calculated based on this file.
                                """.trimMargin()
                        )
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Output Localization File:")
                    outputLocalizationFile = textField()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(model::outputLocalizationFile)
                        .comment("The output localization file, i.e. the file used to import the generated localization class.")
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Output Class:")
                    outputClass = textField()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(model::outputClass)
                        .comment("The output class, i.e. the class which will be generated, most of the time it's AppLocalizations, but also S, which is concise.")
                }.layout(RowLayout.PARENT_GRID)
                row {
                    nullableGetter = checkBox("Nullable Getter")
                        .bindSelected(model::nullableGetter)
                        .comment("If enabled, the generated getter will be nullable, i.e. S.of(context)?.helloWorld instead of S.of(context).helloWorld")
                }
            }
            group("Corrupt Settings") {
                row {
                    //Intellij error icon
                    icon(
                        AllIcons.General.Error
                    )
                    label("")
                        .bindText(model.errorMessage)
                    button("Clear Corrupt Settings") {
                        resetSettings()
                    }
                }
            }.visibleIf(corruptSettingPredicate())

        }
        initSettingsFromSavedSettings()
        return panel!!;
    }

    private fun corruptSettingPredicate() = object : ComponentPredicate() {
        override fun addListener(listener: (Boolean) -> Unit) {
            errorListener = listener
        }

        override fun invoke(): Boolean {
            return corruptSettings
        }
    }

    private fun resetSettings() {
        corruptSettings = false
        errorListener?.invoke(false)
        model.errorMessage.set("")
        userSettingsRepository.wipeUserSettings()
        initSettingsFromSavedSettings()
    }

    private fun initSettingsFromSavedSettings() {
        try {
            val settings = userSettingsRepository.getSettings()
            openAIKeyField.component.text = settings.openAIKey
            intlConfigFile.component.text = settings.intlConfigFile ?: "${currentProjectProvider.project.basePath}/l10n.yaml"
            arbDirectory.component.text = settings.arbDir ?: "${currentProjectProvider.project.basePath}/lib/l10n"
            templateArbFile.component.text = settings.templateArbFile
            outputLocalizationFile.component.text = settings.outputLocalizationFile
            outputClass.component.text = settings.outputClass
            watchIntlConfigFile.component.isSelected = settings.watchIntlConfigFile
            nullableGetter.component.isSelected = settings.nullableGetter
        } catch (e: UserSettingsMissingException) {
            println("looks like we have no settings yet: ${e.message}")
            intlConfigFile.component.text = "${currentProjectProvider.project.basePath}/l10n.yaml"
            return
        } catch (e: UserSettingsCorruptException) {
            corruptSettings = true
            model.errorMessage.set("Looks like the settings are corrupt, do you want to delete clear all settings? ErrorMessage: ${e.message}")
            errorListener?.invoke(true)
        }
    }

    private fun testPassword(text: CharArray) {
        text.fill('0')
        TODO("Not yet implemented")
    }

    private fun savePassword(text: CharArray) {
        userSettingsRepository.overrideSettings { settings ->
            settings.copy(openAIKey = text.joinToString(""))
        }
        text.fill('0')
        openAIKeyField.text("")
    }

    override fun isModified(): Boolean {
        return panel?.isModified() ?: false
    }

    private fun loadIntlSettingsFromFile(text: String) {
        val flutterIntlSettings = try {
            flutterIntlSettingsRepository.loadFlutterIntlSettingsByPath(Path.of(text))
        } catch (e: Exception) {
            DialogBuilder().apply {
                centerPanel(
                    panel {
                        row {
                            label("Could not load the flutter intl settings from the file, please check the file and try again. Error: ${e.message}")
                        }
                        //set text
                        addOkAction().apply {
                            okAction.setText("Close")
                        }
                    }
                )
            }.showModal(true)
            return
        }
        //okay, we have the settings, we can now load them into the UI
        arbDirectory.component.text = flutterIntlSettings.arbDir
        templateArbFile.component.text = flutterIntlSettings.templateArbFile
        outputLocalizationFile.component.text = flutterIntlSettings.outputLocalizationFile
        outputClass.component.text = flutterIntlSettings.outputClass
        nullableGetter.component.isSelected = flutterIntlSettings.nullableGetter
    }

    override fun apply() {
        try {
            Initializer().userSettingsRepository.overrideSettings { settings ->
                settings.copy(
                    intlConfigFile = intlConfigFile.component.text,
                    arbDir = arbDirectory.component.text,
                    templateArbFile = templateArbFile.component.text,
                    outputClass = outputLocalizationFile.component.text,
                    nullableGetter = nullableGetter.component.isSelected,
                    watchIntlConfigFile = watchIntlConfigFile.component.isSelected
                )
            }
        } catch (e: UserSettingsMissingException) {
            Initializer().userSettingsRepository.saveSettings(
                UserSettings(
                    openAIKey = "",
                    intlConfigFile = intlConfigFile.component.text,
                    arbDir = arbDirectory.component.text,
                    templateArbFile = templateArbFile.component.text,
                    outputClass = outputLocalizationFile.component.text,
                    nullableGetter = nullableGetter.component.isSelected,
                    outputLocalizationFile = outputLocalizationFile.component.text
                )
            )
        } catch (e: UserSettingsCorruptException) {
            corruptSettings = true
            model.errorMessage.set("Looks like the settings are corrupt, do you want to delete clear all settings? ErrorMessage: ${e.message}")
            errorListener?.invoke(true)
        }
    }

    internal data class Model(
        var arbDir: String = "",
        var intlConfigFile: String = "",
        var templateArbFile: String = "",
        var outputLocalizationFile: String = "",
        var outputClass: String = "",
        var nullableGetter: Boolean = false,
        var watchIntlConfigFile: Boolean = true,
        var dataCorrupt: Boolean = false,
        var errorMessage: AtomicProperty<String> = AtomicProperty("")
    )
}
