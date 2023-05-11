package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.BasePathSettingMissing
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.TranslationFileNotFound
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import java.nio.file.Path

class LanguageFileFinder(
    private val userSettingsRepository: UserSettingsRepository,
) {
    fun findLanguageFile(language: Language, project: Project): Document {
        val userSettings = userSettingsRepository.getSettings()
        val filePath = getAbsoluteFilePath(language, userSettings, project)
        val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(filePath)
            ?: throw TranslationFileNotFound("File not found.", language, filePath)
        val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
            ?: throw TranslationFileNotFound(
                "We found the file, but not as a PsiFile, e.g. it is a directory, or not a valid file",
                language,
                filePath
            )
        return PsiDocumentManager.getInstance(project).getDocument(psiFile)
            ?: throw TranslationFileNotFound(
                "File found, but could not proceed, because the file is e.g. binary or there is no associated document",
                language,
                filePath
            )
    }

    private fun getAbsoluteFilePath(language: Language, settings: UserSettings, project: Project): Path {
        val translationBaseDir = settings.arbDir ?: throw BasePathSettingMissing("You need to set the translation base directory")
        val filePrefix = getArbFilePrefix(settings.templateArbFile)

        return Path.of(project.basePath!!).resolve(translationBaseDir).resolve("$filePrefix${language.toISOLangString()}.arb")
    }

    private fun getArbFilePrefix(fileName: String): String {
        val regex = Regex("^(.*?)([a-z]{2}(?:_[A-Z]{2})?)?(\\.arb)$")
        val matchResult = regex.matchEntire(fileName)

        return matchResult?.groups?.get(1)?.value ?: ""
    }
}
