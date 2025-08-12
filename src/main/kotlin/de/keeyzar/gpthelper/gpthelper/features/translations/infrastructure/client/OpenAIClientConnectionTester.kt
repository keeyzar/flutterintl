package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.google.genai.types.ListModelsConfig
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientConnectionTester
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.TranslationClientConnectionException
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider

class OpenAIClientConnectionTester(
    private val configProvider: OpenAIConfigProvider
) : ClientConnectionTester {
    /**
     * there might be other issues. We should allow the user to see the issue, therefore return the throwable
     */
    override suspend fun testClientConnection(key: String): Throwable? {
        //like try with
        configProvider.withKeyGemini(key).use {
            try {
                //if this call is successful, everything is fine. we can save some resources
                //by using some specific model, but no time currently in a train.
                it.models.list(ListModelsConfig.builder().pageSize(20).build()).page()
                return null
            } catch (e: Throwable) {
                e.printStackTrace()
                return TranslationClientConnectionException("Could not connect", e)
            }
        }
    }
}
