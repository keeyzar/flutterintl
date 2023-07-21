package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.TranslationRequestResponseMapper
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Disabled
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
    @Mock
    private lateinit var userSettingsRepository: UserSettingsRepository
    private val objectMapper = ObjectMapper();

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this);
        val parallelism = 1
        given(dispatcherConfiguration.getDispatcher())
            .willReturn(Executors.newFixedThreadPool(parallelism).asCoroutineDispatcher())
        given(dispatcherConfiguration.getLevelOfParallelism())
            .willReturn(parallelism)
        given(userSettingsRepository.getSettings()).
            willReturn(UserSettings("", "", false, "", "", false, "", 1, "informal", "gpt-3.5-turbo-0613", true))
        val key = System.getenv("openai_api_key")
        given(openAIConfigProvider.getInstance()).willReturn(OpenAI(OpenAIConfig(
            token = key,
            timeout = Timeout(socket = 60.seconds),
            logLevel = LogLevel.All,
            headers = mapOf(),
        )))
        parser = TranslationRequestResponseMapper(objectMapper)
        sut = GPTTranslationRequestClient(openAIConfigProvider, parser, dispatcherConfiguration, userSettingsRepository)
    }

    @Test
    fun testThatRequestWorks() {
        runBlocking {
            val req = ClientTranslationRequest(
                listOf(
//                    Language("ru", null),
                    Language("de", null),
//                    Language("es_ES", null),
//                    Language("it", null),
//                    Language("pl", null),
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
                assertThat(it).isNotNull
                assertThat(it.translation.entry.desiredKey).isEqualTo("choose_page_scaffold_expert_mode_text")
                assertThat(it.translation.entry.desiredValue)
                    .isNotNull()
                    .isNotEqualTo("Unable to connect to the payments processor. Has this app been configured correctly? See the example README for instructions.")
                assertThat(it.translation.entry.desiredDescription)
                    .isNotNull()
                    .isNotEqualTo("whether to enable expert mode or not")
            }
        }
    }


    @Test
    fun testPlaceholderWorks() {
        runBlocking {
            val req = ClientTranslationRequest(
                listOf(
                    Language("ru", null),
                    Language("de", null),
                ), Translation(
                    Language("en", null),
                    SimpleTranslationEntry(
                        "none",
                        "message_amount",
                        "You have %messageAmount new messages.",
                        "How much messages the user has"
                    )
                )
            )
            sut.requestTranslationOfSingleEntry(req) {
                assertThat(it).isNotNull
                assertThat(it.translation.entry.desiredKey).isEqualTo("message_amount")
                val placeholder = it.translation.entry.placeholder
                //parse placeholder to jsonnode for easy access
                val placeholderJson = objectMapper.valueToTree<JsonNode>(placeholder)
                assertThat(placeholderJson).isNotNull
                assertThat(placeholderJson["messageAmount"]).isNotNull
                assertThat(placeholderJson["messageAmount"]["type"].asText()).isEqualTo("num")
                assertThat(placeholderJson["messageAmount"]["format"].asText()).isEqualTo("compact")
            }
        }
    }

    /**
     * does not yet work, but I bet with gpt 4 it's working
     */
    @Test
    @Disabled
    fun testTwoPlaceholderWorks() {
        runBlocking {
            val req = ClientTranslationRequest(
                listOf(
                    Language("ru", null),
                    Language("de", null),
                ), Translation(
                    Language("en", null),
                    SimpleTranslationEntry(
                        "none",
                        "credits_and_premium",
                        "You have \$credits Credits and you have Premium: \$premiumStatus",
                        "How much credits and premium status of the user"
                    )
                )
            )
            sut.requestTranslationOfSingleEntry(req) {
                assertThat(it).isNotNull
                assertThat(it.translation.entry.desiredKey).isEqualTo("credits_and_premium")
                val placeholder = it.translation.entry.placeholder
                //parse placeholder to jsonnode for easy access
                val placeholderJson = objectMapper.valueToTree<JsonNode>(placeholder)
                assertThat(placeholderJson).isNotNull
                assertThat(placeholderJson["credits"]).isNotNull
                assertThat(placeholderJson["credits"]["type"].asText()).isEqualTo("num")
                assertThat(placeholderJson["credits"]["format"].asText()).isEqualTo("compact")
                assertThat(placeholderJson["premiumStatus"]).isNotNull
                assertThat(placeholderJson["premiumStatus"]["type"].asText()).isEqualTo("String")
            }
        }
    }
}
