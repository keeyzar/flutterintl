package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import com.google.gson.Gson
import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking

class ExistingKeyFinder(
    private val translationFileRepository: TranslationFileRepository,
    private val arbFilesService: ArbFilesService
) {
    fun findExistingKeys(allEntries: List<PsiElement>): Map<PsiElement, String> {
        val lang = ReadAction.compute<Language, RuntimeException> {
            arbFilesService.getBaseLanguage(null)
        }
        val content = translationFileRepository.getTranslationFileByLanguage(lang).content
        val arbMap = Gson().fromJson(content, Map::class.java) as Map<String, String>
        val arbValueToKey = arbMap.entries
            .filterNot { it.key.startsWith("@") }
            .associate { (k, v) -> v to k }

        val elementsWithText = ReadAction.compute<List<Pair<PsiElement, String>>, RuntimeException> {
            allEntries.mapNotNull { element ->
                val text = when (element) {
                    is DartStringLiteralExpression -> element.text.removeQuotes()
                    else -> null
                }
                if (text.isNullOrBlank()) {
                    null
                } else {
                    element to text
                }
            }
        }

        return runBlocking {
            val chunkSize = (elementsWithText.size / 4).coerceAtLeast(1)
            elementsWithText.chunked(chunkSize).map { chunk ->
                async(Dispatchers.Default) {
                    chunk.mapNotNull { (element, text) ->
                        findBestMatch(text, arbValueToKey)?.let { key ->
                            element to key
                        }
                    }
                }
            }.awaitAll().flatten().toMap()
        }
    }

    private fun findBestMatch(text: String, arbValueToKey: Map<String, String>): String? {
        // 1. Exact case-sensitive match for very short strings (e.g., up to 4 characters)
        if (text.length <= 4) {
            return arbValueToKey.entries.find { (value, _) ->
                value == text
            }?.value
        }

        // 2. Case-insensitive exact match for longer strings
        val exactIgnoreCaseMatch = arbValueToKey.entries.find { (value, _) ->
            value.equals(text, ignoreCase = true)
        }
        if (exactIgnoreCaseMatch != null) {
            return exactIgnoreCaseMatch.value
        }

        // 3. Fuzzy match for longer strings with a dynamic threshold
        var bestMatch: String? = null
        // Dynamic threshold: for longer strings, allow for more differences.
        // For a string of length 10, the threshold will be 2. For a length of 5, it's 1.
        var minDistance = (text.length / 5).coerceAtLeast(1)

        for ((value, key) in arbValueToKey) {
            // only compare strings that are not drastically different in length
            if (kotlin.math.abs(text.length - value.length) > minDistance + 1) {
                continue
            }

            val distance = levenshtein(text.lowercase(), value.lowercase())
            if (distance < minDistance) {
                minDistance = distance
                bestMatch = key
            }
        }

        return bestMatch
    }

    /**
     * calculates the levenshtein distance
     */
    private fun levenshtein(lhs: String, rhs: String): Int {
        if (lhs == rhs) {
            return 0
        }
        if (lhs.isEmpty()) {
            return rhs.length
        }
        if (rhs.isEmpty()) {
            return lhs.length
        }

        val lhsLength = lhs.length
        val rhsLength = rhs.length

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

/**
 * on a String, remove all quotes, either ", ' or ''' or """
 */
fun String.removeQuotes(): String {
    return this.removeSurrounding("'''")
        .removeSurrounding("\"\"\"")
        .removeSurrounding("'")
        .removeSurrounding("\"")
}