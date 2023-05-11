package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity.TranslateFileContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.ArbFilenameParser
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.GatherFileTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service.TargetLanguageProvider
import java.util.UUID

class IdeaGatherFileTranslationContext(
    private val contextProvider: ContextProvider,
    private val arbFilenameParser: ArbFilenameParser,
    private val targetLanguageProvider: TargetLanguageProvider,
) : GatherFileTranslationContext {
    override fun gatherTranslationContext(uuid: UUID): TranslateFileContext? {
        val context = contextProvider.getContext(uuid)?: throw IllegalStateException("Context not found, this is a programmer error, sorry")
        val targetLang = targetLanguageProvider.getTargetLanguage()?: return null
        return TranslateFileContext(
            baseLanguage = arbFilenameParser.getLanguageFromPath(context.baseFile.virtualFile.toNioPath()),
            targetLanguage = targetLang,
            context.baseFile.virtualFile.toNioPath()
        )
    }
}
