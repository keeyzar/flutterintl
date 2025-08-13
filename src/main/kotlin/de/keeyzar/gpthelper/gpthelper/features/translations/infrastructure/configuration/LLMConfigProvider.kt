package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration

import com.google.genai.Client
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationCredentialsServiceRepository

class LLMConfigProvider(private val credentialsServiceRepository: TranslationCredentialsServiceRepository) {

    fun getInstanceGemini(): Client {
        val key = credentialsServiceRepository.getKey() ?: throw Exception("API Key missing")
        return withKeyGemini(key)
    }

    fun withKeyGemini(key: String): Client {
        return Client.builder()
            .apiKey(key)
            .build()
    }
}

