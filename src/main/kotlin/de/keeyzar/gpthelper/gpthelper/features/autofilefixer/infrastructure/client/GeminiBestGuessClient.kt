package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.client

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessL10nClient
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessRequest
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessResponse
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.exception.BestGuessClientException
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.parser.BestGuessOpenAIResponseParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.LLMConfigProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class GeminiBestGuessClient(
    private val LLMConfigProvider: LLMConfigProvider,
    private val bestGuessOpenAIResponseParser: BestGuessOpenAIResponseParser,
    private val userSettingsRepository: UserSettingsRepository
) : BestGuessL10nClient {

    companion object {
        const val PARALLELISM_THRESHOLD = 20
        private val parallelGuessSemaphore = Semaphore(3)
    }

    private fun createRequestContent(bestGuessRequest: BestGuessRequest): String {
        val newLineDelimitedLiterals = bestGuessRequest.context.literals
            .fold(StringBuilder()) { acc, literal ->
                acc.appendLine("id: ${literal.id}, context: ${literal.context}")
            }

        return """
            You're an API Server and your task is to provide localization key guesses for the user. Please provide best guess l18n keys for these Strings. These are used in the context of Flutter arb localization

            try to adhere to the following rules for the keys:
            key format: context_type_description
            keys can only be lowercase and underscore, and must begin with a letter.
            The description should explain the intent of the text, not the actual text itself.

            key examples: 
            - loginpage_title_greeting
            - loginpage_label_username
            - loginpage_button_login
            - common_button_cancel
            - common_button_confirm
            
            
            You respond by (BUT WITHOUT WHITESPACE - as to save bandwidth):
            [{
            "id": <identical to the one you received>,
            "key": "<your best guess key>",
            "description": "...",
            {...}
            ]
           
            These are the literals you should process:
            $newLineDelimitedLiterals
        """.trimIndent()
    }

    override suspend fun simpleGuess(bestGuessRequest: BestGuessRequest, progressReport: (() -> Unit)?): BestGuessResponse {
        val literals = bestGuessRequest.context.literals
        if (literals.size <= PARALLELISM_THRESHOLD) {
            return singleRequestGuess(bestGuessRequest, progressReport)
        }

        return parallelGuess(bestGuessRequest, progressReport)
    }

    private suspend fun singleRequestGuess(bestGuessRequest: BestGuessRequest, progressReport: (() -> Unit)?): BestGuessResponse {
        val gemini = LLMConfigProvider.getInstanceGemini()
        val modelId = userSettingsRepository.getSettings().gptModel
        val request = createRequestContent(bestGuessRequest)

        val response = gemini.models.generateContent(
            modelId,
            request,
            null
        )

        val resultText = response.text() ?: throw BestGuessClientException("Gemini hat nicht geantwortet oder die Antwort war leer.")
        progressReport?.invoke()
        return bestGuessOpenAIResponseParser.parse(resultText)
    }

    private suspend fun parallelGuess(bestGuessRequest: BestGuessRequest, progressReport: (() -> Unit)?): BestGuessResponse = coroutineScope {
        val literals = bestGuessRequest.context.literals
        val chunkSize = PARALLELISM_THRESHOLD
        val chunks = literals.chunked(chunkSize)

        val deferredResponses = chunks.map { chunk ->
            async {
                parallelGuessSemaphore.withPermit {
                    val chunkRequest = bestGuessRequest.copy(
                        context = bestGuessRequest.context.copy(
                            literals = chunk
                        )
                    )
                    val response = singleRequestGuess(chunkRequest, progressReport)
                    progressReport?.invoke()
                    response
                }
            }
        }

        val responses = deferredResponses.awaitAll()

        val allGuesses = responses.flatMap { it.responseEntries }
        return@coroutineScope BestGuessResponse(allGuesses)
    }
}
