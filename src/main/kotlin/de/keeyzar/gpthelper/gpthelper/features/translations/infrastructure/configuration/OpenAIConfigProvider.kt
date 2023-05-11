package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import kotlin.time.Duration.Companion.seconds

class OpenAIConfigProvider(private val userSettingsRepository: UserSettingsRepository) {
    fun getInstance(): OpenAI{
        val settings = userSettingsRepository.getSettings()
        val token = settings.openAIKey ?: throw Exception("OpenAI token not found")
        val config = OpenAIConfig(
                token = token,
                timeout = Timeout(socket = 60.seconds),
                logLevel = LogLevel.All,
                headers = mapOf(),
        )
        return OpenAI(config)
    }
}

