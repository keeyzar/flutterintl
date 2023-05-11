package de.keeyzar.gpthelper.gpthelper.features.filetranslation.presentation.service

import com.intellij.openapi.application.ApplicationManager
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service.TargetLanguageProvider
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.presentation.widgets.TargetLanguageDialog
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.ArbFilenameParser
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.LanguageFileFinder

class IdeaTargetLanguageProvider(
    private val arbFilenameParser: ArbFilenameParser
) : TargetLanguageProvider {
    override fun getTargetLanguage(): Language? {
        var lang: Language? = null
        ApplicationManager.getApplication().invokeAndWait {
            val dialog = TargetLanguageDialog()
            val closedWithOk = dialog
                .showAndGet()
            if (closedWithOk) {
                lang = arbFilenameParser.stringToLanguage(dialog.getTargetLanguage())
            }
        }
        return lang
    }
}
