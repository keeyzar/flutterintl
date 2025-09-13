package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search

import com.google.gson.Gson
import java.text.Normalizer

/**
 * Builds an in-memory index from the raw ARB JSON content.
 * Does not perform any IO; accepts the JSON content as a String.
 */
class ArbIndexer {
    private val gson = Gson()

    fun buildIndex(arbJsonContent: String): List<ArbIndexEntry> {
        if (arbJsonContent.isBlank()) return emptyList()

        val parsed = try {
            gson.fromJson(arbJsonContent, Map::class.java) as? Map<String, Any?> ?: return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }

        return parsed.entries
            .asSequence()
            // filter out metadata entries that start with @
            .filter { (k, _) -> !k.startsWith("@") }
            .mapNotNull { (k, v) ->
                val value = v as? String ?: return@mapNotNull null
                val normalized = normalize(value)
                val trigrams = generateTrigrams(normalized)
                ArbIndexEntry(
                    key = k,
                    value = value,
                    normalizedValue = normalized,
                    trigrams = trigrams
                )
            }
            .toList()
    }

    private fun normalize(input: String): String {
        // lowercase and remove diacritics
        val lower = input.lowercase()
        val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return normalized
    }

    private fun generateTrigrams(s: String): Set<String> {
        val cleaned = s.replace("\\s+".toRegex(), " ").trim()
        if (cleaned.length <= 3) return setOf(cleaned)

        val trigrams = mutableSetOf<String>()
        for (i in 0..cleaned.length - 3) {
            trigrams.add(cleaned.substring(i, i + 3))
        }
        return trigrams
    }
}

