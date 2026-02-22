package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search

import com.intellij.openapi.application.ApplicationManager
import java.text.Normalizer
import java.util.concurrent.atomic.AtomicBoolean
import java.util.stream.Collectors

/**
 * Provides fast suggestions for ARB entries. Builds an in-memory index from ARB JSON content
 * and caches it until the content changes.
 *
 * This class performs two-stage search:
 *  - fast filtering using prefix & trigram intersection
 *  - precise scoring using Levenshtein distance
 *
 * Both indexing (via [ArbIndexer]) and search use parallel streams to
 * exploit multiple CPU cores, keeping latency low even for projects
 * with thousands of ARB entries.
 */
class ArbSuggestionService(
    private val arbFileContentProvider: ArbFileContentProvider
) {
    private val indexer = ArbIndexer()
    @Volatile
    private var index: List<ArbIndexEntry> = emptyList()
    @Volatile
    private var lastContentHash: Int = 0
    private val refreshInProgress = AtomicBoolean(false)

    // Pre-compiled patterns – avoids re-creating a Regex object per call
    private val diacriticPattern = Regex("\\p{M}+")
    private val whitespacePattern = Regex("\\s+")

    /**
     * Threshold above which search filters run in parallel.
     * For small indices the overhead of ForkJoinPool is not worth it.
     */
    private val parallelThreshold = 500

    /**
     * Synchronously refreshes the index if the content has changed.
     * Safe to call from any thread.
     */
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

    /**
     * Triggers an asynchronous background refresh of the index.
     * If a refresh is already in progress, this call is a no-op.
     * The current (possibly stale) index remains available for queries while refreshing.
     */
    fun refreshIndexAsync() {
        if (!refreshInProgress.compareAndSet(false, true)) {
            // another refresh is already running – skip
            return
        }
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                refreshIndexIfNeeded()
            } catch (_: Exception) {
                // ignore – indexing will be attempted again on demand
            } finally {
                refreshInProgress.set(false)
            }
        }
    }

    fun suggest(input: String, limit: Int = 10): List<Suggestion> {
        if (input.isBlank()) return emptyList()
        val normalizedInput = normalize(input)

        // If index is empty, try a synchronous refresh so we can return results immediately
        var currentIndex = index
        if (currentIndex.isEmpty()) {
            try {
                refreshIndexIfNeeded()
                currentIndex = index
            } catch (_: Exception) {
                // ignore – we'll return empty
            }
            if (currentIndex.isEmpty()) return emptyList()
        }

        val useParallel = currentIndex.size >= parallelThreshold

        // Fast prefix matching
        val prefixMatches = filterIndex(currentIndex, useParallel) {
            it.normalizedValue.startsWith(normalizedInput)
        }
        val candidates = mutableSetOf<ArbIndexEntry>()
        candidates.addAll(prefixMatches)

        // If we already have enough prefix matches, rank and return
        if (candidates.size >= limit) {
            return rankAndLimit(candidates.toList(), normalizedInput, limit)
        }

        // Trigram matching
        val inputTrigrams = generateTrigramsForSearch(normalizedInput)
        if (inputTrigrams.isNotEmpty()) {
            val trigramMatches = filterIndex(currentIndex, useParallel) { entry ->
                entry.trigrams.any { it in inputTrigrams }
            }
            candidates.addAll(trigramMatches)
        }

        // Fallback substring match
        if (candidates.size < limit) {
            val substringMatches = filterIndex(currentIndex, useParallel) {
                it.normalizedValue.contains(normalizedInput)
            }
            candidates.addAll(substringMatches)
        }

        return rankAndLimit(candidates.toList(), normalizedInput, limit)
    }

    /**
     * Filters [entries] using the given [predicate], automatically choosing
     * a parallel or sequential stream based on [parallel].
     */
    private inline fun filterIndex(
        entries: List<ArbIndexEntry>,
        parallel: Boolean,
        crossinline predicate: (ArbIndexEntry) -> Boolean
    ): List<ArbIndexEntry> {
        val stream = if (parallel) entries.parallelStream() else entries.stream()
        return stream.filter { predicate(it) }.collect(Collectors.toList())
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
            .replace(diacriticPattern, "")
        return normalized
    }

    private fun generateTrigramsForSearch(s: String): Set<String> {
        val cleaned = s.replace(whitespacePattern, " ").trim()
        if (cleaned.length <= 3) return setOf(cleaned)
        val trigrams = HashSet<String>(cleaned.length)
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

                newCost[j] = minOf(costReplace, costInsert, costDelete)
            }

            val swap = cost
            cost = newCost
            newCost = swap
        }

        return cost[lhsLength]
    }
}

