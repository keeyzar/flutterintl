package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.parser

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessResponse
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessResponseEntry
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.exception.BestGuessClientException

class BestGuessOpenAIResponseParser(
    private val objectMapper: ObjectMapper,
) {
    fun parse(response: String): BestGuessResponse {
        return try {
            val entries = objectMapper.readValue<List<BestGuessResponseEntry>>(response)
            BestGuessResponse(responseEntries = entries)
        } catch (e: Throwable){
            e.printStackTrace()
            throw BestGuessClientException("Could not parse the response of chatGPT\nResponse:\n$response")
        }
    }
}
