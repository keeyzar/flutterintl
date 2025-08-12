package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
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

    private var log: Logger = Logger.getInstance(PsiTranslationFileRepository::class.java)

    override fun getTranslationFileByLanguage(language: Language): FileToTranslate {
        log.trace("before reading the file for language: $language")
        val computeAction = ReadAction.compute<FileToTranslate, GPTHelperBaseException> {
            log.trace("inside ReadAction.compute for language: $language")
            val project = currentProjectProvider.project
            val document = languageFileFinder.findLanguageFile(language, project);
            return@compute FileToTranslate(language, document.text)
        }
        log.trace("after reading the file for language: $language")
        return computeAction
    }

    override fun createOrGetTranslationFileByLanguage(language: Language): FileToTranslate {
        var fileToTranslate: FileToTranslate? = null
        ApplicationManager.getApplication().invokeAndWait {
            fileToTranslate = WriteAction.compute<FileToTranslate, GPTHelperBaseException> {
                val project = currentProjectProvider.project
                val file = languageFileFinder.createOrGetLanguageFile(language, project);
                return@compute FileToTranslate(language, file.text)
            }
        }
        return fileToTranslate!!
    }

    override fun saveTranslationFile(fileToTranslate: FileToTranslate) {
        log.trace("in saveTranslationFile, prior to WriteAction.runAndWait")
        WriteAction.runAndWait<GPTHelperBaseException> {
            log.trace("in saveTranslationFile, inside WriteAction.runAndWait")
            CommandProcessor.getInstance().executeCommand(currentProjectProvider.project, {
                log.trace("in saveTranslationFile, inside executeCommand")
                val project = currentProjectProvider.project
                val document = languageFileFinder.findLanguageFile(fileToTranslate.language, project);
                document.setText(fileToTranslate.content)
                // Explicitly save the document to disk
                FileDocumentManager.getInstance().saveDocument(document)
            }, "translation", "translate")
            log.trace("in saveTranslationFile, after executeCommand")
        }
        log.trace("in saveTranslationFile, after WriteAction.runAndWait")
    }

    override fun getPathsToTranslationFiles(): List<Path> {
        val arbDir = userSettingsRepository.getSettings().arbDir ?: throw UserSettingsException("The setting for Arb directory is missing")
        val arbDirAbsolute = "${currentProjectProvider.project.basePath}/$arbDir"
        return File(arbDirAbsolute).listFiles()
            ?.filter { it.isFile && it.extension == "arb" }
            ?.map { it.toPath() } ?: throw UserSettingsException("The setting for Arb directory seems to be incorrect, there is no directory at Path: '$arbDirAbsolute'")
    }

    override fun getPathToFile(language: Language): Path {
        val arbDir = userSettingsRepository.getSettings().arbDir ?: throw UserSettingsException("The setting for Arb directory is missing")
        val arbDirAbsolute = "${currentProjectProvider.project.basePath}/$arbDir"
        return Path.of("$arbDirAbsolute/app_${language.toISOLangString()}.arb")
    }
}
