package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.ui.dsl.builder.panel
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository.FlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.GptHelperSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.UserSettingsCorruptException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.UserSettingsMissingException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.VerifyTranslationSettingsService
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.CurrentProjectProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.validation.FlutterIntlValidator
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.validation.TranslationClientSettingsValidator

/**
 * try to load settings, if there are none, ask the user, whether he wants to use the default settings or something thereof
 */
class ArbVerifyTranslationSettingsService(
    private val userSettingsRepository: UserSettingsRepository,
    //TODO Might be a list here, and you register as many validators as you want?
    private val translationClientSettingsValidator: TranslationClientSettingsValidator,
    private val flutterIntlValidator: FlutterIntlValidator,
    private val flutterIntlSettingsRepository: FlutterIntlSettingsRepository,
    private val currentProjectProvider: CurrentProjectProvider,
) : VerifyTranslationSettingsService {
    override fun verifySettingsAndInformUserIfInvalid(): Boolean {
        val project = currentProjectProvider.project

        val userSettings = try {
            userSettingsRepository.getSettings()
        } catch (e: UserSettingsMissingException) {
            askToNavigateToSettingsPage(project, "Missing Settings", "You need to configure the plugin first.")
            return false
        } catch (e: UserSettingsCorruptException) {
            e.printStackTrace()
            askToNavigateToSettingsPage(
                project,
                "Settings corrupt",
                "There are some issues with the settings, please wipe the data. I'm sorry..."
            )
            return false
        }

        //check whether there are some issues with the settings
        if (validateTranslationClient(userSettings, project)) return false

        return potentiallyRefreshFlutterIntlSettings(userSettings, project)
    }

    private fun potentiallyRefreshFlutterIntlSettings(
        userSettings: UserSettings,
        project: Project
    ): Boolean {
        return try {
            if (userSettings.watchIntlConfigFile) {
                print("User has configured to watch the settings, therefore we update it")
                val flutterSettings = flutterIntlSettingsRepository.getFlutterIntlSettings()
                userSettingsRepository.overrideSettings {
                    it.copy(
                        arbDir = flutterSettings.arbDir,
                        outputClass = flutterSettings.outputClass,
                        nullableGetter = flutterSettings.nullableGetter,
                        templateArbFile = flutterSettings.templateArbFile,
                        outputLocalizationFile = flutterSettings.outputLocalizationFile,
                    )
                }
            } else {
                val errors = flutterIntlValidator.valid(userSettings)
                if (errors.isNotEmpty()) {
                    askToNavigateToSettingsPage(
                        project,
                        "Missing Flutter Intl Settings",
                        "There are some issues with the flutter intl settings, please check the settings page. Error details: ${errors.joinToString("<br>")}"
                    )
                    return false
                }
            }
            true
        } catch (e: Exception) {
            askToNavigateToSettingsPage(
                project,
                "Missing Settings",
                "Translation Settings Missing: Could not find l10n.yaml file in project, error details: ${e.message}"
            )
            false
        }
    }

    private fun validateTranslationClient(
        userSettings: UserSettings,
        project: Project
    ): Boolean {
        val errors = translationClientSettingsValidator.valid(userSettings)
        if (errors.isNotEmpty()) {
            askToNavigateToSettingsPage(
                project,
                "Missing Open AI Key",
                "There is no Open AI Key configured, please configure it in the settings page. Error details: ${errors.joinToString("<br>")}"
            )
            return true
        }
        return false
    }

    private fun askToNavigateToSettingsPage(project: Project, title: String, message: String) {
        ApplicationManager.getApplication().invokeAndWait {
            val closedWithOk = DialogBuilder().apply {
                setTitle(title)
                setCenterPanel(
                    panel {
                        row {
                            label(message)
                        }
                        row {
                            label("Want to go to configuration page?")
                        }
                    }
                )
                addOkAction()
                addCancelAction()
            }.showAndGet()
            if(closedWithOk) {
                ShowSettingsUtil.getInstance().showSettingsDialog(project, GptHelperSettings::class.java)
            }
        }
    }
}
