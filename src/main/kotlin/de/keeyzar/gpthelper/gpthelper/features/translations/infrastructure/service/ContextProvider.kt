package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.model.FileContext
import java.util.*

/**
 * TODO this is an issue, I need to fix... I need some kind of map at least
 */
class ContextProvider {
    val context: MutableMap<UUID, FileContext> = mutableMapOf()
    fun putContext(uuid: UUID, fileContext: FileContext) {
        context.putIfAbsent(uuid, fileContext);
    }
    fun getContext(uuid: UUID): FileContext? {
        return context[uuid]
    }
    fun removeContext(uuid: UUID) {
        context.remove(uuid)
    }
}
