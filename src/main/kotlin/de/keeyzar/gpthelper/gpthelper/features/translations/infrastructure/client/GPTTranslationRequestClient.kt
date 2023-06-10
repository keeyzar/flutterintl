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
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.TranslationRequestResponseMapper
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope


class GPTTranslationRequestClient(
    private val openAIConfigProvider: OpenAIConfigProvider,
    private val translationRequestResponseParser: TranslationRequestResponseMapper,
) : DDDTranslationRequestClient {

    override suspend fun requestTranslationOfSingleEntry(
        clientTranslationRequest: ClientTranslationRequest,
        partialTranslationFinishedCallback: (PartialTranslationResponse) -> Unit,
    ) {
        val baseContent = translationRequestResponseParser.toGPTContent(clientTranslationRequest.translation)

        coroutineScope {
            val deferredTranslations = clientTranslationRequest.targetLanguages.map { targetLang ->
                async {
                    try {
                        val response = requestTranslation(baseContent, targetLang.toISOLangString())
                        val translation = translationRequestResponseParser.fromResponse(targetLang, response, clientTranslationRequest.translation)
                        partialTranslationFinishedCallback(PartialTranslationResponse(translation))
                    } catch (e: Throwable) {
                        throw TranslationRequestException(
                            "Translation request failed for ${targetLang.toISOLangString()} with message: Is the file valid json?", e,
                            Translation(lang = targetLang, clientTranslationRequest.translation.entry)
                        )
                    }
                }
            }
            deferredTranslations.forEach { it.await() }
        }
    }

    @OptIn(BetaOpenAI::class)
    suspend fun requestTranslation(content: String, targetLanguage: String): String {
        val modelToUse = "gpt-3.5-turbo-0301"

        val request = """
            You act as an API Server answering with valid JSON only.
            please translate this localization arb file content to the following languages: $targetLanguage. 
            Do not modify keys. Please feel free to improve the wording. 
            It's in an app context for users of ages 14-99. If you see typos, fix them (except for keys).
            If you see terms e.g. flutter widget terms like "card", "label" in the descriptions, please do not translate them
            Answer only with the new content.
            
            the format is: 
            {"<key1>": "<value1>", ...}}
            
            The content is:
            $content
        """.trimIndent()

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(modelToUse),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.User,
                    content = request
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
