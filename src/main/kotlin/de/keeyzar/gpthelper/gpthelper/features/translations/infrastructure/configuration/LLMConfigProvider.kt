package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration

import com.google.genai.Client
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationCredentialsServiceRepository

class LLMConfigProvider(private val credentialsServiceRepository: TranslationCredentialsServiceRepository) {

    fun getInstanceGemini(): Client {
        return credentialsServiceRepository.getKey()?.let {
            withKeyGemini(it)
        } ?: withoutGeminiKey()
    }

    private fun withoutGeminiKey(): Client {
        return Client.builder()
            .build()
    }

    fun withKeyGemini(key: String): Client {
        return Client.builder()
            .apiKey(key)
            .build()
    }
}

