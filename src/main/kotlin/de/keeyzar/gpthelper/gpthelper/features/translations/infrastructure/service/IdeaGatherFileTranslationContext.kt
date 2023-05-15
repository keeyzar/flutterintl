package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity.TranslateFileContext
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.GatherFileTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service.TargetLanguageProvider
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import java.util.UUID

class IdeaGatherFileTranslationContext(
    private val contextProvider: ContextProvider,
    private val targetLanguageProvider: TargetLanguageProvider,
    private val arbFilesService: ArbFilesService,
) : GatherFileTranslationContext {
    override fun gatherTranslationContext(uuid: UUID): TranslateFileContext? {
        val context = contextProvider.getTranslateWholeFileContext(uuid)?: throw IllegalStateException("Context not found, this is a programmer error, sorry")
        val targetLang = targetLanguageProvider.getTargetLanguage()?: return null
        return TranslateFileContext(
            baseLanguage = arbFilesService.getBaseLanguage(context.baseFile.virtualFile.toNioPath()),
            targetLanguage = targetLang,
            context.baseFile.virtualFile.toNioPath()
        )
    }
}
