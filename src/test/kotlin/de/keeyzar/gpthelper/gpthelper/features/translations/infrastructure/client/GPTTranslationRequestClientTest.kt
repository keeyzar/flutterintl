package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.fasterxml.jackson.databind.ObjectMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.TranslationRequestResponseMapper
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.given
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

class GPTTranslationRequestClientTest {
    @Mock
    private lateinit var openAIConfigProvider: OpenAIConfigProvider
    private lateinit var sut: GPTTranslationRequestClient
    private lateinit var parser: TranslationRequestResponseMapper
    @Mock
    private lateinit var dispatcherConfiguration: DispatcherConfiguration
    private val objectMapper = ObjectMapper();

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this);
        val parallelism = 1
        given(dispatcherConfiguration.getDispatcher())
            .willReturn(Executors.newFixedThreadPool(parallelism).asCoroutineDispatcher())
        given(dispatcherConfiguration.getLevelOfParallelism())
            .willReturn(parallelism)
        parser = TranslationRequestResponseMapper(objectMapper)
        sut = GPTTranslationRequestClient(openAIConfigProvider, parser, dispatcherConfiguration)
    }

    @Test
    fun testThatRequestWorks() {
        runBlocking {
            val key = System.getenv("openai_api_key")
            given(openAIConfigProvider.getInstance()).willReturn(OpenAI(OpenAIConfig(
                token = key,
                timeout = Timeout(socket = 60.seconds),
                logLevel = LogLevel.All,
                headers = mapOf(),
            )))
            given(openAIConfigProvider.getConfiguredModel()).willReturn(ModelId("gpt-3.5-turbo-0613"))

            val req = ClientTranslationRequest(
                listOf(
                    Language("ru", null),
                    Language("de", null),
                    Language("es_ES", null),
                    Language("it", null),
                    Language("pl", null),
                    ), Translation(
                    Language("en", null),
                    SimpleTranslationEntry(
                        "someId",
                        "choose_page_scaffold_expert_mode_text",
                        "Unable to connect to the payments processor. Has this app been configured correctly? See the example README for instructions.",
                        "whether to enable expert mode or not"
                    )
                )
            )
            sut.requestTranslationOfSingleEntry(req) {
                Assertions.assertThat(it).isNotNull
                Assertions.assertThat(it.translation.entry.desiredKey).isEqualTo("choose_page_scaffold_expert_mode_text")
                Assertions.assertThat(it.translation.entry.desiredValue)
                    .isNotNull()
                    .isNotEqualTo("Unable to connect to the payments processor. Has this app been configured correctly? See the example README for instructions.")
                Assertions.assertThat(it.translation.entry.desiredDescription)
                    .isNotNull()
                    .isNotEqualTo("whether to enable expert mode or not")
            }
        }
    }
}
