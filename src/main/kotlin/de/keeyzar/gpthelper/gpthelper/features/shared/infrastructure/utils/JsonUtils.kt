package de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils

import com.fasterxml.jackson.databind.ObjectMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.ReplacementOfTranslationFailedException
import java.util.*

class JsonUtils(private val objectMapper: ObjectMapper) {

    private val trailingCommaRegex = Regex(",\\s*}")


    fun hasAnyEntry(jsonString: String): Boolean {
        return jsonString.contains(":")
    }

    fun removeTrailingCommas(jsonString: String): String {
        val replace = jsonString.replace(trailingCommaRegex, "}").trim()
        if (replace.last() == ',') {
            return replace.dropLast(1)
        }
        return replace
    }

    fun replaceKeys(oldContent: String, simpleTranslationEntry: SimpleTranslationEntry): String {
        val newMap = objectMapper.readValue(oldContent, Map::class.java)
            .toMutableMap()
        entryToMap(simpleTranslationEntry).forEach { (key, value) ->
            if (!newMap.containsKey(key)) {
                throw ReplacementOfTranslationFailedException("Translation failed. Key not found in old translations $key")
            }
            newMap[key] = value
        }
        return objectMapper.writeValueAsString(newMap)
    }

    fun removeTrailingComma(jsonString: String): String {
        return jsonString.trim().removeSuffix(",")
    }

    fun removeSurroundingBrackets(jsonString: String): String {
        return jsonString.trim().removeSurrounding("{", "}")
    }

    fun removeDuplicatedCommas(jsonString: String): String {
        val stack = Stack<Char>()
        var isInsideQuotes = false
        var result = ""
        var lastCharCommaOutsideQuotes = true

        for (c in jsonString) {
            when (c) {
                ',' -> {
                    if (isInsideQuotes) {
                        //we don't really care about commas inside quotes, do whatever you want
                        result += ","
                        stack.push(',')
                    } else if (lastCharCommaOutsideQuotes) {
                        // do nothing, do not push, because now we have a duplicated comma
                    } else {
                        result += ","
                        stack.push(',')
                        lastCharCommaOutsideQuotes = true
                    }
                }

                '"' -> {
                    if (quoteNotEscaped(stack)) {
                        isInsideQuotes = !isInsideQuotes
                    }
                    stack.push('"')
                    result += c
                    lastCharCommaOutsideQuotes = false
                }

                else -> {
                    stack.push(c)
                    result += c
                    if (isNotWhitespace(c)) {
                        lastCharCommaOutsideQuotes = false
                    }
                }
            }
        }

        return result
    }

    private fun isNotWhitespace(c: Char): Boolean {
        return c != ' ' && c != '\n' && c != '\t' && c != '\r'
    }

    private fun quoteNotEscaped(stack: Stack<Char>) = stack.isNotEmpty() && stack.peek() != '\\'

    fun prettify(newContentCleaned: String): String {
        val content = objectMapper.readTree(newContentCleaned)
        return objectMapper.writeValueAsString(content)
    }


    private fun entryToMap(simpleTranslationEntry: SimpleTranslationEntry) = simpleTranslationEntry.toMap()
    fun entryToJsonString(simpleTranslationEntry: SimpleTranslationEntry): String = objectMapper.writeValueAsString(simpleTranslationEntry.toMap())
    fun entryToJsonStringWithoutSurroundingBrackets(simpleTranslationEntry: SimpleTranslationEntry) =
        removeSurroundingBrackets(entryToJsonString(simpleTranslationEntry))

    fun SimpleTranslationEntry.toMap(): Map<String, *> {
        val map = mutableMapOf<String, Any>()
        map[this.desiredKey] = this.desiredValue
        map["@" + this.desiredKey] = mapOf("description" to this.desiredDescription)
        return map;
    }
}
