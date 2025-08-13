package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.google.genai.types.ListModelsConfig
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.GPTModelProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.LLMConfigProvider
import kotlinx.coroutines.runBlocking

class GptModelProviderImpl (
    private val LLMConfigProvider: LLMConfigProvider,
) : GPTModelProvider() {
    override fun getAllModels(): List<String> {
        return runBlocking {
            LLMConfigProvider.getInstanceGemini().models.list(ListModelsConfig.builder().pageSize(20).build()).page()
                //TODO is there a possibility to filter by type? :) because we do not want to list e.g. whisper
                .map { e -> e.name().orElse("no name") }
                .toList();
        }
    }

}
