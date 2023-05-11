package de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity.TranslateFileContext
import java.util.UUID

/**
 * gather information required to request the information from the user
 */
fun interface GatherFileTranslationContext {
    fun gatherTranslationContext(uuid: UUID): TranslateFileContext?
}
