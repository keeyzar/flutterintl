package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.DDDTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.PartialTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.TranslationRequestException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.TranslationRequestResponseMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class GPTTranslationRequestClient(
    private val openAIConfigProvider: OpenAIConfigProvider,
    private val translationRequestResponseParser: TranslationRequestResponseMapper,
    private val dispatcherConfiguration: DispatcherConfiguration,
    private val userSettingsRepository: UserSettingsRepository,
    ) : DDDTranslationRequestClient {

    override suspend fun requestTranslationOfSingleEntry(
        clientTranslationRequest: ClientTranslationRequest,
        partialTranslationFinishedCallback: (PartialTranslationResponse) -> Unit,
    ) {
        val shouldUseAdvancedMethod = userSettingsRepository.getSettings().translateAdvancedArbKeys
        val baseContent = if(shouldUseAdvancedMethod) {
            translationRequestResponseParser.toGPTContentAdvanced(clientTranslationRequest.translation)
        } else {
            translationRequestResponseParser.toGPTContent(clientTranslationRequest.translation)
        }
        val dispatcher = dispatcherConfiguration.getDispatcher()
        val parallelism = dispatcherConfiguration.getLevelOfParallelism()
        coroutineScope {
            val deferredTranslations = clientTranslationRequest.targetLanguages.chunked(parallelism).flatMap { chunk ->
                chunk.map { targetLang ->
                    async(dispatcher) {
                        try {
                            retryCall(2) {
                                val response = if(shouldUseAdvancedMethod) {
                                    requestComplexTranslation(baseContent, targetLang.toISOLangString())
                                } else {
                                    requestTranslation(baseContent, targetLang.toISOLangString())
                                }
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
        val tonality = userSettingsRepository.getSettings().tonality
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
            target Language: "DE"
        """.trimIndent()
        val initialAnswer = """
            {"expert_not_active":"Nicht aktiviert.","@expert_not_active":{"description":"Nicht aktivierter Zustand"}}
        """.trimIndent()

        val realRequest = """
            $content
            Please translate to target language: "$targetLanguage" and with tonality: "$tonality"
        """.trimIndent()
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(userSettingsRepository.getSettings().gptModel),
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

    @OptIn(BetaOpenAI::class)
    suspend fun requestComplexTranslation(content: String, targetLanguage: String): String {
        val tonality = userSettingsRepository.getSettings().tonality
        val initialRequest = """
            Can you please provide a flutter arb entry based on the content?
            There are different kinds of localization possibilities, either plain simple, without a variable
            text: "You have the option to enable this setting"
            key: "some_key"
            description: "explains user what is happening"
            tonality: informal
            targetLanguage: de
        """.trimIndent()
        val initialAnswer = """
             {"some_key" : "Du hast die Möglichkeit diese Einstellung zu aktivieren",
             "@some_key" : {
               "description" : "Erklärt dem Nutzer was passiert",
             }}
        """.trimIndent()
        val secondRequest = """
            text: "There are ${'$'}wombats"
            key: "nWombats"
            description: "A plural message"
            tonality: formal
            target Language: en
        """.trimIndent()
        val secondResponse = """
            {"nWombats": "There are {count, plural, =0{no wombats} =1{1 wombat} other{{count} wombats}}",
            "@nWombats": {
              "description": "A plural message",
              "placeholders": {
                "count": {
                  "type": "num",
                  "format": "compact"
                }
              }
            }}
        """.trimIndent()

        val thirdRequest = """
            text: You are ${'$'}gender
            key: "pronoun"
            description: "A gendered message"
            targetLanguage: en
        """.trimIndent()

        val thirdResponse = """
            {"pronoun": "You are {gender, select, male{he} female{she} other{they}}",
            "@pronoun": {
              "description": "A gendered message",
              "placeholders": {
                "gender": {
                  "type": "String"
                }
              }}
        """.trimIndent()

        val realRequest = """
            $content
            Please take care of plural, if required, target language: "$targetLanguage" and with tonality: "$tonality. Do not add placeholder if it is not required"
        """.trimIndent()
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(userSettingsRepository.getSettings().gptModel),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You are responsible for internationalization of flutter strings. Because the user does not exactly know what he needs, you need to make a best guess regarding the target translation"
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
                    content = secondRequest
                ),
                ChatMessage(
                    role = ChatRole.Assistant,
                    content = secondResponse
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = thirdRequest
                ),
                ChatMessage(
                    role = ChatRole.Assistant,
                    content = thirdResponse
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
