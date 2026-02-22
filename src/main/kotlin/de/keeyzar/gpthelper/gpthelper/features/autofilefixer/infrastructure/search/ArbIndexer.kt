package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search

import com.google.gson.Gson
import java.text.Normalizer
import java.util.stream.Collectors

/**
 * Builds an in-memory index from the raw ARB JSON content.
 * Does not perform any IO; accepts the JSON content as a String.
 *
 * Uses parallel streams so that large ARB files are indexed across
 * multiple CPU cores, significantly reducing wall-clock time for
 * projects with thousands of translation keys.
 */
class ArbIndexer {
    private val gson = Gson()

    // Pre-compiled patterns – avoids re-creating a Regex object per entry
    private val diacriticPattern = Regex("\\p{M}+")
    private val whitespacePattern = Regex("\\s+")

    fun buildIndex(arbJsonContent: String): List<ArbIndexEntry> {
        if (arbJsonContent.isBlank()) return emptyList()

        val parsed = try {
            gson.fromJson(arbJsonContent, Map::class.java) as? Map<String, Any?> ?: return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }

        // Filter out metadata entries, then build index entries in parallel
        val entries = parsed.entries
            .filter { (k, v) -> !k.startsWith("@") && v is String }

        return entries.parallelStream()
            .map { (k, v) ->
                val value = v as String
                val normalized = normalize(value)
                val trigrams = generateTrigrams(normalized)
                ArbIndexEntry(
                    key = k,
                    value = value,
                    normalizedValue = normalized,
                    trigrams = trigrams
                )
            }
            .collect(Collectors.toList())
    }

    private fun normalize(input: String): String {
        val lower = input.lowercase()
        val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace(diacriticPattern, "")
        return normalized
    }

    private fun generateTrigrams(s: String): Set<String> {
        val cleaned = s.replace(whitespacePattern, " ").trim()
        if (cleaned.length <= 3) return setOf(cleaned)

        val trigrams = HashSet<String>(cleaned.length)
        for (i in 0..cleaned.length - 3) {
            trigrams.add(cleaned.substring(i, i + 3))
        }
        return trigrams
    }
}

