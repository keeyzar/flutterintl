package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.DDDTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.PartialTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.TranslationRequestException
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.TranslationRequestResponseMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.Executors


class GPTTranslationRequestClient(
    private val openAIConfigProvider: OpenAIConfigProvider,
    private val translationRequestResponseParser: TranslationRequestResponseMapper,
    private val dispatcherConfiguration: DispatcherConfiguration,
) : DDDTranslationRequestClient {

    override suspend fun requestTranslationOfSingleEntry(
        clientTranslationRequest: ClientTranslationRequest,
        partialTranslationFinishedCallback: (PartialTranslationResponse) -> Unit,
    ) {
        val baseContent = translationRequestResponseParser.toGPTContent(clientTranslationRequest.translation)
        val dispatcher = dispatcherConfiguration.getDispatcher()
        val parallelism = dispatcherConfiguration.getLevelOfParallelism()
        coroutineScope {
            val deferredTranslations = clientTranslationRequest.targetLanguages.chunked(parallelism).flatMap { chunk ->
                chunk.map { targetLang ->
                    async(dispatcher) {
                        try {
                            retryCall(2) {
                                val response = requestTranslation(baseContent, targetLang.toISOLangString())
                                val translation = translationRequestResponseParser.fromResponse(targetLang, response, clientTranslationRequest.translation)
                                partialTranslationFinishedCallback(PartialTranslationResponse(translation))
                            }
                        } catch (e: Throwable) {
                            throw TranslationRequestException(
                                "Translation request failed for ${targetLang.toISOLangString()} with message: Is the file valid json?", e,
                                Translation(lang = targetLang, clientTranslationRequest.translation.entry)
                            )
                        }
                    }
                }
            }
            deferredTranslations.forEach { it.await() }
        }
    }

    private suspend fun retryCall(retries: Int, block: suspend () -> Unit) {
        var currentTry = 0
        while (currentTry < retries) {
            try {
                block()
                return
            } catch (e: Throwable) {
                currentTry++
            }
        }
        throw Exception("Failed after $retries retries")
    }

    @OptIn(BetaOpenAI::class)
    suspend fun requestTranslation(content: String, targetLanguage: String): String {
        val initialRequest = """
            Please Translate the following content. Do not modify keys. Please feel free to improve the wording, but stay consistent with length.
            It's in an app context for users of ages 14-50. If you see typos, fix them (except for keys).
            If you see terms e.g. flutter widget terms like "card", "label" in the 'description', please do not translate them.
            Answer only with the new content.

            Content:
            {
                "expert_not_active": "Not activated.",
                "@expert_not_active": {
                    "description": "Not activated state"
                }
            }
            target Language: DE
        """.trimIndent()
        val initialAnswer = """
            {"expert_not_active":"Nicht aktiviert.","@expert_not_active":{"description":"Nicht aktivierter Zustand"}}
        """.trimIndent()

        val realRequest = """
            $content
            target language: $targetLanguage
        """.trimIndent()
        val chatCompletionRequest = ChatCompletionRequest(
            model = openAIConfigProvider.getConfiguredModel(),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You are a JSON Api Translator. You are given a JSON file and a target language. Your task is to translate the content of the JSON file to the target language, even if it is the same language."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = initialRequest
                ),
                ChatMessage(
                    role = ChatRole.Assistant,
                    content = initialAnswer
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = realRequest
                )
            )
        )

        val openAI = openAIConfigProvider.getInstance();
        val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest);
        if (completion.choices.isEmpty()) {
            throw Exception("Could not translate content");
        }
        //this is a single response
        return completion.choices[0].message?.content!!
    }
}
