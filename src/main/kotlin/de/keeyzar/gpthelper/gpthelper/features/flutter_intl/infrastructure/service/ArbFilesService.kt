package de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service

import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.NoTranslationFilesException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.ArbFilenameParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import java.nio.file.Path

/**
 * helper class providing a lot of functions to work with arb files
 * TODO can be a facade for multiple minor services, which are not exposed to the outside
 */
class ArbFilesService(
    private val arbFilenameParser: ArbFilenameParser,
    private val translationFileRepository: TranslationFileRepository,
    private val project: Project,
    private val userSettingsRepository: UserSettingsRepository,
) {

    fun findAvailableLanguages(): List<Language> {
        val pathsToTranslationFiles = translationFileRepository.getPathsToTranslationFiles()
        if (pathsToTranslationFiles.isEmpty()) {
            throw NoTranslationFilesException("There are no translation files in your project, you might want to create some")
        }
        return pathsToTranslationFiles
            .map { arbFilenameParser.getLanguageFromPath(it) }
    }

    /**
     * either you provide a path which we should get the language from, or it'll be returned from the userSettings configured basePath
     */
    fun getBaseLanguage(path: Path?): Language {
        val definitivePath = when (path) {
            null -> {
                val relativePath = userSettingsRepository.getSettings().templateArbFile
                Path.of("${project.basePath}/$relativePath")
            }
            else -> path
        }
        return arbFilenameParser.getLanguageFromPath(definitivePath)
    }

}
