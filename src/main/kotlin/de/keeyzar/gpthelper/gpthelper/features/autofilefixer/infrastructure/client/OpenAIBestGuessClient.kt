package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.client

import com.aallam.openai.api.BetaOpenAI
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessL10nClient
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessRequest
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessResponse
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessResponseEntry
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.exception.BestGuessClientException
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.parser.BestGuessOpenAIResponseParser
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider

class OpenAIBestGuessClient(
    private val openAIConfigProvider: OpenAIConfigProvider,
    private val bestGuessOpenAIResponseParser: BestGuessOpenAIResponseParser,
) : BestGuessL10nClient {

    /**
     * TODO make this template configurable, might be that the user wants to override the template we send
     */
    private fun createRequestContent(bestGuessRequest: BestGuessRequest): String {
        val fileName = bestGuessRequest.context.filename
        val newLineDelimitedLiterals = bestGuessRequest.context.literals
            .fold(StringBuilder()) { acc, literal ->
                acc.appendLine("id: ${literal.id}, context: ${literal.context}")
            }

        return """
            You're an API Server and your task is to provide localization key guesses for the user. Please provide best guess l18n keys for these Strings. These are used in the context of Flutter arb localization
            You respond by (BUT WITHOUT WHITESPACE):
            [{
            "id": <identical to the one you received>,
            "key": "..."
            "description": "..."
            },
            {...}
            ]

            keys can only be lowercase and underscore, and must begin with a letter.
            The description should explain the intent of the text, not the actual text
            Please ensure that your key follows industry best practice naming patterns

            Filename: $fileName
            $newLineDelimitedLiterals
        """.trimIndent()
    }

    @OptIn(BetaOpenAI::class)
    override suspend fun simpleGuess(bestGuessRequest: BestGuessRequest): BestGuessResponse {
        //this class has a fairly simple task. get the open AI stuff, and make the request, parse it again
        val openAI = openAIConfigProvider.getInstance()
        val modelId = openAIConfigProvider.getConfiguredModel()

        val request = createRequestContent(bestGuessRequest)
        val completion = openAI.chatCompletion(
            ChatCompletionRequest(
                modelId, listOf(
                    ChatMessage(
                        role = ChatRole.User,
                        content = request,
                    )
                )
            )
        )
        if(completion.choices.isEmpty()){
            throw BestGuessClientException("Open AI has not responded")
        }

        val response = completion.choices[0].message?.content?: throw BestGuessClientException("Looks like something in the utilized open ai kotlin library has changed. We could not work with the API, but we got a response, as it seems. Sorry. Please file an Issue")
        return bestGuessOpenAIResponseParser.parse(response)
    }
}
