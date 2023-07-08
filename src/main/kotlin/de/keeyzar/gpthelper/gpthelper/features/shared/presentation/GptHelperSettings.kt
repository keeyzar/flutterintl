package de.keeyzar.gpthelper.gpthelper.features.shared.presentation

import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.progress.util.BackgroundTaskUtil.submitTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ComponentPredicate
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository.FlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.mapper.UserSettingsDTOMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.CurrentProjectProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.UserSettingsPersistentStateComponent
import kotlinx.coroutines.runBlocking
import java.nio.file.Path
import javax.swing.JCheckBox
import javax.swing.JComponent

/**
 * TODO I think I made some mistakes here.
 */
class GptHelperSettings(val project: Project) : Configurable {
    private var panel: DialogPanel? = null
    private lateinit var openAIKeyField: Cell<JBPasswordField>
    private lateinit var translationParallelism: Cell<JBTextField>
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
    private lateinit var settingsMapper: UserSettingsDTOMapper
    private var corruptSettings = false;
    private var errorListener: ((Boolean) -> Unit)? = null
    private var connectionErrorListener: ((Boolean) -> Unit)? = null
    private var connectionSuccessListener: ((Boolean) -> Unit)? = null
    private var connectionSuccess = false;
    private val myModel = MyModel()

    override fun getDisplayName(): String = "GPTHelper Settings"
    override fun createComponent(): JComponent {
        userSettingsRepository = Initializer().userSettingsRepository
        flutterIntlSettingsRepository = Initializer().flutterIntlSettingsRepository
        currentProjectProvider = Initializer().currentProjectProvider
        settingsMapper = Initializer().userSettingsDTOMapper


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
                row("Connection failed") {
                    textArea()
                        .bindText(myModel::connectionTestErrorStackTrace)
                        .enabled(false)
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                }.visibleIf(connectionErrorPredicate())
                row("Success, don't forget to press save!") {
                }.visibleIf(connectionSuccessPredicate())
            }
            group("OpenAI Client Configuration") {
                row {
                    label("Translation parallelism")
                    translationParallelism = cell(JBTextField())
                        .bindIntText(UserSettingsPersistentStateComponent.getInstance().state::parallelism)
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .comment("Each file is translated in parallel. A new openAI Account can only make 3 parallel requests. If your Account is older than a week, I suggest increasing to 10 (or how many translation files you have)")
                }.layout(RowLayout.PARENT_GRID)
            }
            group("Flutter Intl") {
                row {
                    label("Intl config file")
                    intlConfigFile = textFieldWithBrowseButton()
                        .bindText(UserSettingsPersistentStateComponent.getInstance().state::intlConfigFile)
                        .resizableColumn()
                        .horizontalAlign(HorizontalAlign.FILL)
                    button("Refresh Settings Based On Intl Config File") {
                        loadIntlSettingsFromFile(intlConfigFile.component.text)
                    }
                }
                row {
                    watchIntlConfigFile = checkBox("Watch intl config file")
                        .bindSelected(UserSettingsPersistentStateComponent.getInstance().state::watchIntlConfigFile)
                        .comment("If enabled, the settings will be refreshed automatically if the intl config file changes. (I.e. on each invoke of the plugin.")

                }
                separator()
                row {
                    label("Arb directory:")
                    arbDirectory = textFieldWithBrowseButton()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .resizableColumn()
                        .bindText(UserSettingsPersistentStateComponent.getInstance().state::arbDir)
                        .comment("The directory where the arb files are located")
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Template arb file:")
                    templateArbFile = textFieldWithBrowseButton()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(UserSettingsPersistentStateComponent.getInstance().state::templateArbFile)
                        .comment(
                            """
                            |The template arb file, i.e. which is the main arb file, most of the time it's app_en.arb.
                            | Based on the template arb file, the other file names will be generated.
                            | Missing translations are calculated based on this file.
                                """.trimMargin()
                        )
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Output localization file:")
                    outputLocalizationFile = textField()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(UserSettingsPersistentStateComponent.getInstance().state::outputLocalizationFile)
                        .comment("The output localization file, i.e. the file used to import the generated localization class.")
                }.layout(RowLayout.PARENT_GRID)
                row {
                    label("Output class:")
                    outputClass = textField()
                        .horizontalAlign(HorizontalAlign.FILL)
                        .bindText(UserSettingsPersistentStateComponent.getInstance().state::outputClass)
                        .comment("The output class, i.e. the class which will be generated, most of the time it's AppLocalizations, but also S, which is concise.")
                }.layout(RowLayout.PARENT_GRID)
                row {
                    nullableGetter = checkBox("Nullable getter")
                        .bindSelected(UserSettingsPersistentStateComponent.getInstance().state::nullableGetter)
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
                        .bindText(myModel.errorMessage)
                    button("Clear Corrupt Settings") {
                        wipeSettings()
                    }
                }
            }.visibleIf(corruptSettingPredicate())

        }
        panel!!.apply() //no changes yet
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


    private fun connectionErrorPredicate() = object : ComponentPredicate() {
        override fun addListener(listener: (Boolean) -> Unit) {
            connectionErrorListener = listener
        }

        override fun invoke(): Boolean {
            return myModel.connectionTestError
        }
    }


    private fun connectionSuccessPredicate() = object : ComponentPredicate() {
        override fun addListener(listener: (Boolean) -> Unit) {
            connectionSuccessListener = listener
        }

        override fun invoke(): Boolean {
            return connectionSuccess
        }
    }

    private fun wipeSettings() {
        corruptSettings = false
        errorListener?.invoke(false)
        myModel.errorMessage.set("")
        userSettingsRepository.wipeUserSettings()
    }


    override fun reset() {
        panel?.reset()
    }


    private fun testPassword(text: CharArray) {
        submitTask({}, {
            runBlocking {
                val it: Throwable? = try {
                    Initializer().connectionTester.testClientConnection(text.concatToString())
                } catch (e: Throwable) {
                    println("eh what")
                    e
                }
                when (it) {
                    null -> {
                        myModel.connectionTestErrorStackTrace = ""
                        myModel.connectionTestError = false
                        connectionErrorListener?.invoke(false)
                        connectionSuccess = true
                        connectionSuccessListener?.invoke(true)
                    }
                    else -> {
                        myModel.connectionTestErrorStackTrace = "Message:" + it.message + "\n\nStackTrace:\n" + it.stackTraceToString()
                        myModel.connectionTestError = true
                        connectionErrorListener?.invoke(true)
                        //i guess must be called from EDT?
                        connectionSuccess = false
                        connectionSuccessListener?.invoke(false)
                    }
                }
            }
        })

    }

    private fun savePassword(text: CharArray) {
        Initializer().credentialsServiceRepository.persistKey(text.concatToString())
        text.fill('0')
        openAIKeyField.text("")
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
        panel?.apply()
    }

    override fun isModified(): Boolean {
        return panel?.isModified()?: false
    }


    //ok, these values are only set, when we save, how annoying is this?
    //binding therefore is not what I had hoped
    internal data class MyModel(
        var dataCorrupt: Boolean = false,
        var errorMessage: AtomicProperty<String> = AtomicProperty(""),
        var connectionTestError: Boolean = false,
        var connectionTestErrorStackTrace: String = "",
    )
}
