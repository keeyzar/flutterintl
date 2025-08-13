package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.google.genai.errors.ApiException
import com.google.genai.errors.ClientException
import com.google.genai.types.Content
import com.google.genai.types.GenerateContentConfig
import com.google.genai.types.Part
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.FileTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.PartialFileTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.DDDTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.PartialTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.SingleTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.TranslationRequestException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.TranslationRequestResponseMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.JsonFileChunker
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlin.random.Random


class GPTTranslationRequestClient(
    private val openAIConfigProvider: OpenAIConfigProvider,
    private val translationRequestResponseParser: TranslationRequestResponseMapper,
    private val dispatcherConfiguration: DispatcherConfiguration,
    private val userSettingsRepository: UserSettingsRepository,
    private val jsonFileChunker: JsonFileChunker,
    private val singleTranslationRequestClient: SingleTranslationRequestClient,
) : DDDTranslationRequestClient {



    suspend fun requestComplexTranslationLong(content: String, targetLanguage: String): String {
        val tonality = userSettingsRepository.getSettings().tonality
        val initialRequest = """
            The goal: 
            create a fitting and syntactically correct arb entry for internationalization
            
            The input:
            The user provides an arb entry key, value and a description. Don't change the key.
            Target language: 
            
            
            Output:
            syntactically correct arb entry Answer in JSON, without spaces to save tokens
            
            
            Reference for arb entries
            simple entry:
            {
              "helloWorld": "Hello World!",
              "@helloWorld": {
                "description": "The conventional newborn programmer greeting"
              }
            }
            
            pluralization
            "{countPlaceholder, plural, =0{message0} =1{message1} =2{message2} few{messageFew} many{messageMany} other{messageOther}}"
            "nWombats": "{count, plural, =0{no wombats} =1{1 wombat} other{{count} wombats}}",
            "@nWombats": {
              "description": "A plural message",
              "placeholders": {
                "count": {
                  "type": "num",
                  "format": "compact"
                }
              }
            }
            
            select syntax
            "{selectPlaceholder, select, case{message} ... other{messageOther}}"
            "pronoun": "{gender, select, male{he} female{she} other{they}}",
            "@pronoun": {
              "description": "A gendered message",
              "placeholders": {
                "gender": {
                  "type": "String"
                }
              }
            }
            
            
            currency and number placeholders:
            compact	"1.2M"
            compactCurrency*	"${'$'}1.2M"
            compactSimpleCurrency*	"${'$'}1.2M"
            compactLong	"1.2 million"
            currency*	"USD1,200,000.00"
            decimalPattern	"1,200,000"
            decimalPatternDigits*	"1,200,000"
            decimalPercentPattern*	"120,000,000%"
            percentPattern	"120,000,000%"
            scientificPattern	"1E6"
            simpleCurrency*	"${'$'}1,200,000"
            Example
            "numberOfDataPoints": "Number of data points: {value}",
            "@numberOfDataPoints": {
              "description": "A message with a formatted int parameter",
              "placeholders": {
                "value": {
                  "type": "int",
                  "format": "compactCurrency",
                  "optionalParameters": {
                    "decimalDigits": 2
                  }
                }
              }
            }
            A message with a date
            "helloWorldOn": "Hello World on {date}",
            "@helloWorldOn": {
              "description": "A message with a date parameter",
              "placeholders": {
                "date": {
                  "type": "DateTime",
                  "format": "yMd"
                }
              }
            }
            Please translate the value to ISO CODE en, tonality: $tonality
            
            
            Target Language: ISO CODE $targetLanguage, tonality: $tonality
            User Input:
            key: "upgrade_popup_name"
            value: "Upgrade achieved"
            description: "User has received an upgrade"
        """.trimIndent()
        val initialAnswer = """
            {"upgrade_popup_name": "Upgrade achieved","@upgrade_popup_name": {"description": "User has received an upgrade"}}
        """.trimIndent()

        val realRequest = """
            Target Language: ISO CODE $targetLanguage, tonality: $tonality 
            User Input:
            $content
        """.trimIndent()

        val history = listOf(
            Content.builder().role("user").parts(Part.builder().text(initialRequest).build()).build(),
            Content.builder().role("model").parts(Part.builder().text(initialAnswer).build()).build(),
            Content.builder().role("user").parts(Part.builder().text(realRequest).build()).build()
        )

        val gemini = openAIConfigProvider.getInstanceGemini()
        val model = userSettingsRepository.getSettings().gptModel
        val config = GenerateContentConfig.builder()
            .systemInstruction(
                Content.builder().parts(
                    Part.builder().text(
                        """
                        You're a flutter intl expert. You return valid intl translations, but you need to guess the most fitting one. You answer only in JSON. If you encounter variables in the value (e.g. ${'$'}{y.x}, ${'$'}x, ${'$'}{x} you must make a valid placeholderName out of that => {x}
                        A valid placeholderName is always letters only.
                    """
                    )
                ).build()
            ).build()

        val response = retryWithBackoff {
            gemini.models.generateContent(model, history, config)
        }
        return response.text() ?: throw Exception("Could not translate content")
    }

    private suspend fun requestTranslationOnly(content: String, targetLanguage: String): String {
        val tonality = userSettingsRepository.getSettings().tonality
        val request = """
            Can you please translate this flutter arb entry into the language '$targetLanguage' (ISO 639-1 Code language code)? do not change keys.
            $content
            the translated tonality should be $tonality . Remember, you're an API server responding in valid JSON only and not in Markdown!. Do not change special chars, e.g. new lines (e.g. \n should stay \n). Thank you so much!
        """.trimIndent()

        val history = listOf(
            Content.builder().role("user").parts(Part.builder().text(request).build()).build()
        )

        val gemini = openAIConfigProvider.getInstanceGemini()
        val model = userSettingsRepository.getSettings().gptModel
        val config = GenerateContentConfig.builder()
            .systemInstruction(Content.builder().parts(Part.builder().text("You are a helpful REST API Server answering in valid JSON, that creates flutter INTL ARB entries.")).build()).build()

        val response = retryWithBackoff {
            gemini.models.generateContent(model, history, config)
        }

        val responseText = response.text() ?: throw Exception("Could not translate content")

        return removeMarkdown(responseText)
    }

    /**
     * because somehow chatgpt is annoyingly trying to be smart and showing it in 
     */
    private fun removeMarkdown(completionResponse: String): String {
        var response = completionResponse.removePrefix("```json\n")
        response = response.removeSuffix("\n```")
        return response
    }


    /**
     * use this as to initially convert to the fitting arb entry
     */
    override suspend fun createComplexArbEntry(clientTranslationRequest: ClientTranslationRequest): PartialTranslationResponse {
        val baseContent = translationRequestResponseParser.toGPTContentAdvanced(clientTranslationRequest.translation)
        val targetLang = clientTranslationRequest.translation.lang
        try {
            val requestComplexTranslation = requestComplexTranslationLong(baseContent, targetLang.toISOLangString())
            val response: Translation =
                translationRequestResponseParser.fromResponse(targetLang, requestComplexTranslation, clientTranslationRequest.translation)
            return PartialTranslationResponse(response)
        } catch (e: Exception) {
            throw TranslationRequestException(
                "Failed to create complex arb entry for ${clientTranslationRequest.translation.entry.desiredKey}",
                e,
                clientTranslationRequest.translation
            )
        }
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
                            val response = requestTranslationOnly(baseContent, targetLang.toISOLangString())
                            val translation =
                                translationRequestResponseParser.fromTranslationOnlyResponse(targetLang, response, partialTranslationResponse.translation)

                            if (!isCancelled()) {
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

            deferredTranslations.forEach {
                if (!isCancelled()) {
                    it.await()
                }
            }
        }
    }

    private suspend fun <T> retryWithBackoff(
        times: Int = 4,
        initialDelay: Long = Random.nextLong(5000, 15001), // 5 to 15 seconds
        maxDelay: Long = 60000, // 60 seconds
        factor: Double = 2.0,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelay
        repeat(times - 1) {
            try {
                return block()
            } catch (e: ApiException) {
                if (e.code() == 429) {
                    delay(currentDelay)
                    currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
                } else {
                    throw e
                }
            } catch (e: ClientException) {
                // Also retry on general client exceptions which might be transient network issues
                delay(currentDelay)
                currentDelay = (currentDelay * factor).toLong().coerceAtMost(maxDelay)
            }
        }
        return block() // last attempt
    }

    override suspend fun translate(
        fileTranslationRequest: FileTranslationRequest,
        partialTranslationFinishedCallback: (PartialFileTranslationResponse, taskSize: Int) -> Unit
    ) {
        val chunkedStrings = jsonFileChunker.chunkJsonBasedOnTotalStringSize(fileTranslationRequest.content, 500);
        // request translation for each chunk in parallel
        //not sure, whether this will block the caller
        val requestSemaphore = Semaphore(9)
        coroutineScope {
            return@coroutineScope chunkedStrings.map {
                async {
                    requestSemaphore.withPermit {
                        val response = singleTranslationRequestClient.requestTranslation(it, fileTranslationRequest.baseLanguage, fileTranslationRequest.targetLanguage)
                        partialTranslationFinishedCallback(PartialFileTranslationResponse(response.content), chunkedStrings.size+1)
                    }
                }
            }
        }
    }

}
