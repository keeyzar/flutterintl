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
        // 1. Case-insensitive exact match
        val exactIgnoreCaseMatch = arbValueToKey.entries.find { (value, _) ->
            value.equals(text, ignoreCase = true)
        }
        if (exactIgnoreCaseMatch != null) {
            return exactIgnoreCaseMatch.value
        }

        // 2. Fuzzy match
        var bestMatch: String? = null
        var minDistance = 3 // Threshold is 3, so we search for anything below that

        for ((value, key) in arbValueToKey) {
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
