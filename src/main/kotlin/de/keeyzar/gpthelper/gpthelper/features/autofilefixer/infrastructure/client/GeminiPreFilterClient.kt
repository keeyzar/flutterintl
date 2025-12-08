package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.client

import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.ThinkingConfig
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.PreFilterClient
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterLiteral
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterRequest
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterResponse
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterResult
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.exception.BestGuessClientException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client.DispatcherConfiguration
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.LLMConfigProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*

class GeminiPreFilterClient(
    private val llmConfigProvider: LLMConfigProvider,
    private val userSettingsRepository: UserSettingsRepository,
    private val dispatcherConfiguration: DispatcherConfiguration
) : PreFilterClient {

    companion object {
        const val MAX_TOKENS_PER_BATCH = 5000
        const val ESTIMATED_TOKENS_PER_LITERAL = 250 // Conservative estimate
    }

    private fun createRequestContent(literals: List<PreFilterLiteral>): String {
        val literalsJson = literals.joinToString(",\n") { literal ->
            """
            {
              "id": "${literal.id.replace("\"", "\\\"")}",
              "literal": "${literal.literalText.replace("\"", "\\\"").replace("\n", "\\n")}",
              "context": "${literal.context.replace("\"", "\\\"").replace("\n", "\\n")}"
            }
            """.trimIndent()
        }

        return """
            You are an API that pre-filters string literals for Flutter localization. 
            
            Your task: Determine which literals should be translated (shown to users) vs. which are technical (config, keys, debug, etc.).
            
            Rules:
            - Translate: UI text, user-facing messages, labels, buttons, titles, descriptions
            - Don't translate: Technical strings, file paths, config keys, debug output, variable names, empty strings, single characters like "[]", "/", etc.
            
            Examples:
            "hello user" in Text("hello user") → true (UI text)
            "name/${'$'}userChoice" in storeValue(...) → false (config/path)
            "[]" in var x = "[]" → false (technical value)
            "Login" in ElevatedButton(...) → true (button text)
            "var x = 'No information required'" → true (text definition used later)
            
            Respond with ONLY a JSON array (no markdown, no extra text):
            [
              {"id": "1", "shouldTranslate": true},
              {"id": "2", "shouldTranslate": false}
            ]
            
            Literals to analyze:
            [
            $literalsJson
            ]
        """.trimIndent()
    }

    override suspend fun preFilter(
        request: PreFilterRequest,
        progressCallback: ((Int, Int) -> Unit)?
    ): PreFilterResponse {
        val batches = createBatches(request.literals)

        if (batches.size == 1) {
            progressCallback?.invoke(1, 1)
            return processSingleBatch(batches[0])
        }

        return processMultipleBatches(batches, progressCallback)
    }

    private fun createBatches(literals: List<PreFilterLiteral>): List<List<PreFilterLiteral>> {
        val batches = mutableListOf<List<PreFilterLiteral>>()
        var currentBatch = mutableListOf<PreFilterLiteral>()
        var currentTokenCount = 0

        for (literal in literals) {
            val estimatedTokens = estimateTokens(literal)

            if (currentTokenCount + estimatedTokens > MAX_TOKENS_PER_BATCH && currentBatch.isNotEmpty()) {
                batches.add(currentBatch)
                currentBatch = mutableListOf()
                currentTokenCount = 0
            }

            currentBatch.add(literal)
            currentTokenCount += estimatedTokens
        }

        if (currentBatch.isNotEmpty()) {
            batches.add(currentBatch)
        }

        return batches
    }

    private fun estimateTokens(literal: PreFilterLiteral): Int {
        // Simple estimation: count characters and divide by 4 (rough average for tokens)
        val textLength = literal.literalText.length + literal.context.length
        return (textLength / 4).coerceAtLeast(ESTIMATED_TOKENS_PER_LITERAL)
    }

    private fun processSingleBatch(literals: List<PreFilterLiteral>): PreFilterResponse {
        val gemini = llmConfigProvider.getInstanceGemini()
        val modelId = userSettingsRepository.getSettings().gptModel
        val request = createRequestContent(literals)
        val thinkingBudget = getThinkingBudget(modelId)
        val response = gemini.models.generateContent(
            modelId,
            request,
            GenerateContentConfig.builder()
                .thinkingConfig(
                    ThinkingConfig.builder()
                        .thinkingBudget(thinkingBudget)
                        .build()
                )
                .build()
        )

        val resultText = response.text()
            ?: throw BestGuessClientException("Gemini returned no response for pre-filtering")

        return parseResponse(resultText)
    }

    private fun getThinkingBudget(modelId: String): Int {
        if(modelId.contains("flash")) {
            return 0 //no need
        } else {
            return -1 //automatically define thinking budget
        }
    }

    private suspend fun processMultipleBatches(
        batches: List<List<PreFilterLiteral>>,
        progressCallback: ((Int, Int) -> Unit)?
    ): PreFilterResponse = coroutineScope {
        val totalBatches = batches.size
        var completedBatches = 0
        val dispatcher = dispatcherConfiguration.getDispatcher()
        val parallelism = dispatcherConfiguration.getLevelOfParallelism()

        // Process batches in chunks according to configured parallelism
        val allResponses = mutableListOf<PreFilterResponse>()

        batches.chunked(parallelism).forEach { batchChunk ->
            val deferredResponses = batchChunk.map { batch ->
                async(dispatcher) {
                    val response = processSingleBatch(batch)
                    completedBatches++
                    progressCallback?.invoke(completedBatches, totalBatches)
                    response
                }
            }

            val chunkResponses = deferredResponses.awaitAll()
            allResponses.addAll(chunkResponses)
        }

        val allResults = allResponses.flatMap { it.results }
        PreFilterResponse(allResults)
    }

    private fun parseResponse(responseText: String): PreFilterResponse {
        try {
            // Clean the response - remove markdown code blocks if present
            val cleanedText = responseText
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val jsonArray = Json.parseToJsonElement(cleanedText).jsonArray

            val results = jsonArray.map { element ->
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content
                    ?: throw BestGuessClientException("Missing 'id' in response")
                val shouldTranslate = obj["shouldTranslate"]?.jsonPrimitive?.boolean
                    ?: throw BestGuessClientException("Missing 'shouldTranslate' in response")
                val reason = obj["reason"]?.jsonPrimitive?.contentOrNull

                PreFilterResult(id, shouldTranslate, reason)
            }

            return PreFilterResponse(results)
        } catch (e: Exception) {
            throw BestGuessClientException("Failed to parse pre-filter response: ${e.message}\nResponse was: $responseText", e)
        }
    }
}

