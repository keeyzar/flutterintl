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
        val baseContent = if (shouldUseAdvancedMethod) {
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
                                val response = if (shouldUseAdvancedMethod) {
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
        var lastException: Throwable? = null
        while (currentTry < retries) {
            try {
                block()
                return
            } catch (e: Throwable) {
                currentTry++
                lastException = e
            }
        }
        throw Exception("Failed after $retries retries", lastException)
    }

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

        val openAI = openAIConfigProvider.getInstance()
        val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
        if (completion.choices.isEmpty()) {
            throw Exception("Could not translate content")
        }
        //this is a single response
        return completion.choices[0].message.content!!
    }

    private suspend fun requestComplexTranslation(content: String, targetLanguage: String): String {
        val tonality = userSettingsRepository.getSettings().tonality
        val initialRequest = """Can you please provide a flutter arb entry based on the content?
            There are different kinds of localization possibilities, either plain simple, without a variable
            text: "You have the option to enable this setting"
            key: "some_key"
            description: "explains user what is happening"
            targetLanguage: en
            add plural, if required, add placeholders, if required, the tonality is informal
        """.trimIndent()
        val initialAnswer = """
             {"some_key" : "Du hast die Möglichkeit diese Einstellung zu aktivieren",
             "@some_key" : {
               "description" : "Erklärt dem Nutzer was passiert",
             }}
        """.trimIndent()
        val secondRequest = """
            text: "There are ${'$'}{user.wombatState.amount}"
            key: "nWombats"
            description: "A plural message"
            target Language: en
            add plural, if required, add placeholders, if required, the tonality is informal
        """.trimIndent()
        val secondResponse = """
            "nWombats": "There are {user_wombatState_amount, plural, =0{no wombats} =1{1 wombat} other{{user_wombatState_amount} wombats}}",
            "@nWombats": {
              "description": "A plural message",
              "placeholders": {
                "user_wombatState_amount": {
                  "type": "num",
                  "format": "compact"
                }
              }
            }
        """.trimIndent()

        val thirdRequest = """
            text: You are ${'$'}gender
            key: "pronoun"
            description: "A gendered message"
            targetLanguage: en
            add plural, if required, add placeholders, if required, the tonality is informal
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
            target language: $targetLanguage
            add plural, if required, add placeholders, if required, the tonality is: $tonality
            - Check if you did it correct
        """.trimIndent()
        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(userSettingsRepository.getSettings().gptModel),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You are responsible for internationalization of flutter strings. Internationalization can be done with or without plural.\n" +
                            "Types are: String, num(with format compact), DateTime (with format, e.g. yMd)"
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

        val openAI = openAIConfigProvider.getInstance()
        val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
        if (completion.choices.isEmpty()) {
            throw Exception("Could not translate content")
        }
        //this is a single response
        return completion.choices[0].message.content!!
    }

    suspend fun requestComplexTranslationLong(content: String, targetLanguage: String): String {
        val tonality = userSettingsRepository.getSettings().tonality
        val initialRequest = """
            Can you create the most fitting flutter intl arb json entry for me?

            just for your reminder, an arb entry looks like this

              "page_title_browse_image" : "Browse Images",
              "@page_title_browse_image" : {
                "description" : "page title showing images"
              },

            a more complex example looks like this:
              "reward_dialog_text" : "You just got {count, plural, =0{no credits} =1{1 credit} other{{count} credits}}",
              "@reward_dialog_text" : {
                "description" : "A message indicating the number of credits received",
                "placeholders" : {
                  "count" : {
                    "type" : "num",
                    "format" : "compact"
                  }
                }
              },

            in general, you either have simple text without placeholders, or you have placeholders. When you have placeholders, then you can decide between
            "Hello {username}"
            with the corresponding placeholder username, or you need to pluralize
            "You just got {count, plural, =0{no credits} =1{1 credit} other{{count} credits}}"
            with the corresponding placeholder count
            the format is
            {placeholderName, plural, =AMOUNT{text} =OTHER_AMOUNT{other text {placeholderName}}

            the type is either num, string

            now, please create a fitting example - the description sometimes gives you hints

            key: "upgrade_popup_name"
            value: "Upgrade achieved"
            description: "User has received an upgrade"
            Please translate the value to ISO CODE en, tonality: formal
            Do not change special chars, e.g. new lines (e.g. \n should stay \n)
            Thank you for your help! :)
        """.trimIndent()
        val initialAnswer = """{
            "upgrade_popup_name": "Upgrade achieved",
            "@upgrade_popup_name": {
              "description": "User has received an upgrade"
            }
            }
        """.trimIndent()

        val realRequest = """
            Target Language: ISO CODE $targetLanguage, tonality: $tonality 
            $content
        """.trimIndent()

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(userSettingsRepository.getSettings().gptModel),
            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = """
                        You're a flutter intl expert. You return valid intl translations, but you need to guess the most fitting one. You answer only in JSON. If you encounter variables in the value (e.g. ${'$'}{y.x}, ${'$'}x, ${'$'}{x} you must make a valid placeholderName out of that => {x}
                        A valid placeholderName is always letters only.
                    """.trimIndent()

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

        val openAI = openAIConfigProvider.getInstance()
        val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
        if (completion.choices.isEmpty()) {
            throw Exception("Could not translate content")
        }
        //this is a single response
        return completion.choices[0].message.content!!
    }

    private suspend fun requestTranslationOnly(content: String, targetLanguage: String): String {
        val tonality = userSettingsRepository.getSettings().tonality
        val request = """
            Can you please translate this flutter arb entry into the language '$targetLanguage' (ISO 639-1 Code language code)? do not change keys.
            $content
            the translated tonality should be $tonality . Remember, you're an API server responding in valid JSON only and not in Markdown!. Do not change special chars, e.g. new lines (e.g. \n should stay \n). Thank you so much!
        """.trimIndent()

        val chatCompletionRequest = ChatCompletionRequest(
            model = ModelId(userSettingsRepository.getSettings().gptModel),

            messages = listOf(
                ChatMessage(
                    role = ChatRole.System,
                    content = "You are a helpful REST API Server answering in valid JSON, that creates flutter INTL ARB entries."
                ),
                ChatMessage(
                    role = ChatRole.User,
                    content = request
                )
            )
        )

        val openAI = openAIConfigProvider.getInstance()
        val completion: ChatCompletion = openAI.chatCompletion(chatCompletionRequest)
        if (completion.choices.isEmpty()) {
            throw Exception("Could not translate content")
        }
        val response = removeMarkdown(completion.choices[0].message.content!!)
        //this is a single response
        return response
    }

    /**
     * because somehow chatgpt is annoyingly trying to be smart and showing it in markdown
     */
    private fun removeMarkdown(completionResponse: String): String {
        var response = completionResponse.removePrefix("```json\n")
        response = response.removeSuffix("\n```")
        return response
    }


    /**
     * use this as to initially convert to the fitting arb entry
     */
    override suspend fun createARBEntry(clientTranslationRequest: ClientTranslationRequest): PartialTranslationResponse {
        val baseContent = translationRequestResponseParser.toGPTContentAdvanced(clientTranslationRequest.translation)
        val targetLang = clientTranslationRequest.translation.lang
        val requestComplexTranslation = requestComplexTranslationLong(baseContent, targetLang.toISOLangString())
        val response: Translation = translationRequestResponseParser.fromResponse(targetLang, requestComplexTranslation, clientTranslationRequest.translation)
        return PartialTranslationResponse(response)
    }

    override suspend fun translateValueOnly(
        clientTranslationRequest: ClientTranslationRequest,
        partialTranslationResponse: PartialTranslationResponse,
        isCancelled: () -> Boolean,
        partialTranslationFinishedCallback: (PartialTranslationResponse) -> Unit,
    ) {
        if (isCancelled()) {
            return
        }

        val baseContent = translationRequestResponseParser.toTranslationOnly(partialTranslationResponse.translation)
        val dispatcher = dispatcherConfiguration.getDispatcher()
        val parallelism = dispatcherConfiguration.getLevelOfParallelism()
        val allLanguagesExceptBaseLanguage = clientTranslationRequest.targetLanguages.filter { it != clientTranslationRequest.translation.lang }

        coroutineScope {
            val deferredTranslations = allLanguagesExceptBaseLanguage.chunked(parallelism).flatMap { chunk ->
                chunk.map { targetLang ->
                    async(dispatcher) {
                        if (isCancelled()) {
                            return@async
                        }

                        try {
                            retryCall(2) {
                                val response = requestTranslationOnly(baseContent, targetLang.toISOLangString())
                                val translation =
                                    translationRequestResponseParser.fromTranslationOnlyResponse(targetLang, response, partialTranslationResponse.translation)

                                if (!isCancelled()) {
                                    partialTranslationFinishedCallback(PartialTranslationResponse(translation))
                                }
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

            deferredTranslations.forEach {
                if (!isCancelled()) {
                    it.await()
                }
            }
        }
    }
}
