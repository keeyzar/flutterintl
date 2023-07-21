package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.GPTModelProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider
import kotlinx.coroutines.runBlocking

class GptModelProviderImpl (
    private val openAIConfigProvider: OpenAIConfigProvider,
) : GPTModelProvider() {
    override fun getAllModels(): List<String> {
        return runBlocking {
            openAIConfigProvider.getInstance().models().stream()
                //TODO is there a possibility to filter by type? :) because we do not want to list e.g. whisper
                .map { e -> e.id.id }
                .toList();
        }
    }

}
