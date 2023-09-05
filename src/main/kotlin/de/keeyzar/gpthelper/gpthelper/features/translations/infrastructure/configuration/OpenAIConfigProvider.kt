package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationCredentialsServiceRepository
import kotlin.time.Duration.Companion.seconds

class OpenAIConfigProvider(private val credentialsServiceRepository: TranslationCredentialsServiceRepository) {
    fun getInstance(): OpenAI {
        val key = credentialsServiceRepository.getKey() ?: throw Exception("OpenAI API Key missing")
        return withKey(key)
    }

    fun withKey(key: String): OpenAI{
        val currentThread = Thread.currentThread()
        val originalClassLoader = currentThread.contextClassLoader
        val pluginClassLoader = this::class.java.classLoader
        val openAI: OpenAI?
        try {
//            currentThread.contextClassLoader = pluginClassLoader
            val config = OpenAIConfig(
                token = key,
                timeout = Timeout(socket = 60.seconds),
                logLevel = LogLevel.All,
                headers = mapOf(),
            )
            openAI = OpenAI(config)
        } finally {
//            currentThread.contextClassLoader = originalClassLoader
        }

        return openAI ?: throw Exception("OpenAI API Key missing")
    }

    /**
     * if null, will retrieve the current instance
     * TODO make configurable from outside, because we can't update each time, when there is a new model
     */
    fun getConfiguredModel(): ModelId{
        return ModelId("gpt-3.5-turbo-0613")
    }
}

