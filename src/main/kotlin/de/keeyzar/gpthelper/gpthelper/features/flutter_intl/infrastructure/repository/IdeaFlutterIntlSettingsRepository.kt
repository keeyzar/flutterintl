package de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.repository

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.entity.FlutterIntlSettings
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.exceptions.FlutterIntlFileParseException
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository.FlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.CurrentProjectProvider
import java.nio.file.Path

class IdeaFlutterIntlSettingsRepository(
    private val currentProjectProvider: CurrentProjectProvider,
    private val objectMapper: ObjectMapper,
    private val userSettingsRepository: UserSettingsRepository,
    private val flutterFileRepository: FlutterFileRepository,
) : FlutterIntlSettingsRepository {
    companion object {
        const val DEFAULT_FLUTTER_INTL_CONFIG_FILE = "l10n.yaml"
    }

    override fun getFlutterIntlSettings(): FlutterIntlSettings {
        //throws fileNotFoundException
        val userSettings = try {
            userSettingsRepository.getSettings()
        } catch (e: Exception) {
            //no issue, we just try to find the base setting, if this also does not work, well..
            println("Failed to load userSettings, there might be none ${e.message}. Trying to load default settings");
            null
        }
        val absolutePath = userSettings?.intlConfigFile ?: DEFAULT_FLUTTER_INTL_CONFIG_FILE
        val project = currentProjectProvider.project
        return load(project, Path.of(absolutePath))
    }

    override fun loadFlutterIntlSettingsByPath(path: Path): FlutterIntlSettings {
        return load(currentProjectProvider.project, path)
    }

    private fun load(project: Project, filePath: Path): FlutterIntlSettings {
        val flutterFileContent = flutterFileRepository.getFileContent(project, filePath)

        try {
            return objectMapper.readValue(flutterFileContent, FlutterIntlSettings::class.java)
        } catch (e: Throwable) {
            if(e is MismatchedInputException && e.message?.contains("No content to map") == true){
                //if the file is there, but empty, we will receive some weird errors
                println("File is empty, returning default settings")
                return FlutterIntlSettings()
            }
            throw FlutterIntlFileParseException("Could not parse flutter intl settings file at path $filePath", e);
        }
    }
}
