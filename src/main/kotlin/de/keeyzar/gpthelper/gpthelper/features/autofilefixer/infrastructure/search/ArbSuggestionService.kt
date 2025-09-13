package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import com.google.gson.Gson
import java.text.Normalizer
import kotlin.math.max

/**
 * Provides fast suggestions for ARB entries. Builds an in-memory index from ARB JSON content
 * and caches it until the content changes.
 *
 * This class performs two-stage search:
 *  - fast filtering using prefix & trigram intersection
 *  - precise scoring using Levenshtein distance
 */
class ArbSuggestionService(
    private val arbFileContentProvider: ArbFileContentProvider
) {
    private val indexer = ArbIndexer()
    private var index: List<ArbIndexEntry> = emptyList()
    private var lastContentHash: Int = 0

    fun refreshIndexIfNeeded() {
        println("index check")
        val content = arbFileContentProvider.getBaseLangContent()
        val contentHash = content.hashCode()
        if (contentHash != lastContentHash) {
            index = indexer.buildIndex(content)
            lastContentHash = contentHash
            println("indexing done")
        }
    }

    fun suggest(input: String, limit: Int = 10): List<Suggestion> {
        if (input.isBlank()) return emptyList()
        val normalizedInput = normalize(input)

        // ensure index is fresh
        // Note: callers should call refreshIndexIfNeeded from a background thread when appropriate
        if (index.isEmpty()) {
            refreshIndexIfNeeded()
        }

        // Fast prefix matching
        val prefixMatches = index.filter { it.normalizedValue.startsWith(normalizedInput) }
        val candidates = mutableSetOf<ArbIndexEntry>()
        candidates.addAll(prefixMatches)

        // If we already have enough prefix matches, rank and return
        if (candidates.size >= limit) {
            return rankAndLimit(candidates.toList(), normalizedInput, limit)
        }

        // Trigram matching
        val inputTrigrams = generateTrigramsForSearch(normalizedInput)
        if (inputTrigrams.isNotEmpty()) {
            val trigramMatches = index.asSequence()
                .filter { entry -> entry.trigrams.any { it in inputTrigrams } }
                .toList()
            candidates.addAll(trigramMatches)
        }

        // Fallback substring match
        if (candidates.size < limit) {
            val substringMatches = index.filter { it.normalizedValue.contains(normalizedInput) }
            candidates.addAll(substringMatches)
        }

        return rankAndLimit(candidates.toList(), normalizedInput, limit)
    }

    private fun rankAndLimit(entries: List<ArbIndexEntry>, normalizedInput: String, limit: Int): List<Suggestion> {
        return entries
            .map { entry ->
                val distance = levenshtein(normalizedInput, entry.normalizedValue)
                // smaller distance -> higher score. Also boost prefix matches.
                val prefixBoost = if (entry.normalizedValue.startsWith(normalizedInput)) 0.0 else 5.0
                val rawScore = 100.0 / (1.0 + distance) + (10.0 - prefixBoost)
                Suggestion(entry.key, entry.value, rawScore)
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    private fun normalize(input: String): String {
        val lower = input.lowercase()
        val normalized = Normalizer.normalize(lower, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
        return normalized
    }

    private fun generateTrigramsForSearch(s: String): Set<String> {
        val cleaned = s.replace("\\s+".toRegex(), " ").trim()
        if (cleaned.length <= 3) return setOf(cleaned)
        val trigrams = mutableSetOf<String>()
        for (i in 0..cleaned.length - 3) {
            trigrams.add(cleaned.substring(i, i + 3))
        }
        return trigrams
    }

    // Levenshtein distance implementation
    private fun levenshtein(lhs: String, rhs: String): Int {
        val lhsLength = lhs.length
        val rhsLength = rhs.length

        if (lhsLength == 0) return rhsLength
        if (rhsLength == 0) return lhsLength

        var cost = IntArray(lhsLength + 1) { it }
        var newCost = IntArray(lhsLength + 1) { 0 }

        for (i in 1..rhsLength) {
            newCost[0] = i

            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1

                newCost[j] = maxOf(costReplace, costInsert, costDelete).let { // bugfix: use minOf
                    // placeholder to satisfy Kotlin; will correct below
                    0
                }
            }

            // Correct implementation: compute min for each j
            // We'll recompute properly to avoid logic bug
            // Re-implement the inner loop properly
            // redo loop
            for (j in 1..lhsLength) {
                val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = minOf(costReplace, costInsert, costDelete)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[lhsLength]
    }
}

