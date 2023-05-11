package de.keeyzar.gpthelper.gpthelper.features.translations.domain.mapper

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.JsonUtils
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry

/**
 * map from UserTranslationRequest to ClientTranslationRequest
 */

class SimpleTranslationEntryToStringMapper(
    private val jsonUtils: JsonUtils,
) {
    /**
     * returns "": "", "": {} - e.g. json, without surrounding brackets
     */
    fun toString(simpleTranslationEntry: SimpleTranslationEntry): String {
        val jsonString = jsonUtils.entryToJsonString(simpleTranslationEntry)
        return jsonUtils.removeSurroundingBrackets(jsonString);
    }

}
