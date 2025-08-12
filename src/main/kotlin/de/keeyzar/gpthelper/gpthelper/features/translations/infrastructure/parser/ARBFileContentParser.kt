package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.parser

import com.fasterxml.jackson.databind.ObjectMapper
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.JsonUtils
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.ARBFileContent
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.dto.GPTArbTranslationResponse

class ARBFileContentParser(private val jacksonMapper: ObjectMapper, private val jsonUtils: JsonUtils) {
    fun toARB(lang: String, response: GPTArbTranslationResponse): ARBFileContent {
        var content = jacksonMapper.writeValueAsString(response.content)
        //{..},
        content = jsonUtils.removeTrailingComma(content)
        //{"":""}, (maybe), but take care of "": {"":""}
        if(content.trim().startsWith("{")) {
            content = jsonUtils.removeSurroundingBrackets(content)
        }
        //"":"",
        content = jsonUtils.removeTrailingComma(content)
        //"":""
        return ARBFileContent(lang, content)
    }
}

