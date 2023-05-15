package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.model.AutoLocalizeContext
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.model.TranslateWholeFileContext
import java.util.*

/**
 * TODO this is an issue, I need to fix... I need some kind of map at least
 */
class ContextProvider {
    private val wholeFileContext: MutableMap<UUID, TranslateWholeFileContext> = mutableMapOf()
    private val autoLocalizeContext: MutableMap<UUID, AutoLocalizeContext> = mutableMapOf()
    fun putTranslateWholeFileContext(uuid: UUID, translateWholeFileContext: TranslateWholeFileContext) {
        wholeFileContext.putIfAbsent(uuid, translateWholeFileContext);
    }

    fun getTranslateWholeFileContext(uuid: UUID): TranslateWholeFileContext? {
        return wholeFileContext[uuid]
    }

    fun removeWholeFileContext(uuid: UUID) {
        wholeFileContext.remove(uuid)
    }

    fun putAutoLocalizeContext(uuid: UUID, autoLocalizeContext: AutoLocalizeContext) {
        this.autoLocalizeContext.putIfAbsent(uuid, autoLocalizeContext);
    }

    fun getAutoLocalizeContext(uuid: UUID): AutoLocalizeContext? {
        return autoLocalizeContext[uuid]
    }

    fun removeAutoLocalizeContext(uuid: UUID) {
        autoLocalizeContext.remove(uuid)
    }
}
