package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletion
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.ARBTemplateService
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.parser.GPTARBResponseParser
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.dto.GPTArbTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.SingleTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language


class GPTARBRequester(
    private val templater: ARBTemplateService,
    private val openAIConfigProvider: OpenAIConfigProvider,
    private val responseParser: GPTARBResponseParser,
) : SingleTranslationRequestClient {
    override suspend fun requestTranslation(content: String, targetLanguage: String): GPTArbTranslationResponse {
        return longDescriptionTranslation(content, targetLanguage)
    }

    override suspend fun requestTranslation(content: String, baseLanguage: Language, targetLanguage: Language): GPTArbTranslationResponse {
        return longDescriptionTranslation(content, targetLanguage.toISOLangString())
    }

    override suspend fun requestTranslation(key: String, value: String, description: String, targetLanguage: String): GPTArbTranslationResponse {
        return longDescriptionTranslation(templater.fillTemplate(key, value, description), targetLanguage)
    }

    private suspend fun longDescriptionTranslation(content: String, targetLanguage: String): GPTArbTranslationResponse {

        val request = """
            You act as an API Server answering with valid JSON only.
            please translate this localization arb file content to the following language: $targetLanguage. 
            Do not modify keys. Please feel free to improve the wording. 
            It's in an app context for users of ages 14-99. If you see typos, fix them (except for keys).
            If you see terms e.g. flutter widget terms like "card", "label" in the descriptions, please do not translate them
            Answer only with the new content.
            
            the format is: 
            {"<key1>": "<value1>", ...}}
            
            The content is:
        """.trimIndent()

        return translateWithGPTTurbo(request, content)
    }

    @OptIn(BetaOpenAI::class)
    suspend fun translateWithGPTTurbo(requestTemplate: String, content: String): GPTArbTranslationResponse {
        val modelToUse = openAIConfigProvider.getConfiguredModel()

        val request = """
            $requestTemplate
            $content
        """.trimIndent()

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(modelToUse.id),
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

        return responseParser.parseResponse(completion.choices[0].message?.content!!)
    }
}
