package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.GPTHelperBaseException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.FileToTranslate
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.UserSettingsException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import java.io.File
import java.nio.file.Path

/**
 * accesses the file by Psi from Intellij
 */
class PsiTranslationFileRepository(
    private val currentProjectProvider: CurrentProjectProvider,
    private val languageFileFinder: LanguageFileFinder,
    private val userSettingsRepository: UserSettingsRepository,
) : TranslationFileRepository {
    override fun getTranslationFileByLanguage(language: Language): FileToTranslate {
        return ReadAction.compute<FileToTranslate, GPTHelperBaseException> {
            val project = currentProjectProvider.project
            val document = languageFileFinder.findLanguageFile(language, project);
            return@compute FileToTranslate(language, document.text)
        }
    }

    override fun saveTranslationFile(fileToTranslate: FileToTranslate) {
        WriteAction.runAndWait<GPTHelperBaseException> {
            val project = currentProjectProvider.project
            val document = languageFileFinder.findLanguageFile(fileToTranslate.language, project);
            document.setText(fileToTranslate.content)
        }
    }

    override fun getPathsToTranslationFiles(): List<Path> {
        val arbDir = userSettingsRepository.getSettings().arbDir ?: throw UserSettingsException("The setting for Arb directory is missing")
        val arbDirAbsolute = "${currentProjectProvider.project.basePath}/$arbDir"
        return File(arbDirAbsolute).listFiles()
            ?.filter { it.isFile && it.extension == "arb" }
            ?.map { it.toPath() } ?: throw UserSettingsException("The setting for Arb directory seems to be incorrect, there is no directory at Path: '$arbDirAbsolute'")
    }
}