package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationCredentialsServiceRepository
import kotlin.time.Duration.Companion.seconds

class OpenAIConfigProvider(private val credentialsServiceRepository: TranslationCredentialsServiceRepository) {
    fun getInstance(): OpenAI{
        val key = credentialsServiceRepository.getKey()
        val token = key ?: throw Exception("OpenAI API Key missing")
        val config = OpenAIConfig(
                token = token,
                timeout = Timeout(socket = 60.seconds),
                logLevel = LogLevel.All,
                headers = mapOf(),
        )
        return OpenAI(config)
    }

    fun withKey(key: String): OpenAI{
        val config = OpenAIConfig(
            token = key,
            timeout = Timeout(socket = 60.seconds),
            logLevel = LogLevel.All,
            headers = mapOf(),
        )
        return OpenAI(config)
    }

    /**
     * if null, will retrieve the current instance
     * TODO make configurable from outside, because we can't update each time, when there is a new model
     */
    fun getConfiguredModel(): ModelId{
        return ModelId("gpt-3.5-turbo-0301")
    }
}

