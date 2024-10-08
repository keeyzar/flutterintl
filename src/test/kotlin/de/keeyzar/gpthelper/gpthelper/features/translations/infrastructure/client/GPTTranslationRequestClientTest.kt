package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.aallam.openai.api.http.Timeout
import com.aallam.openai.api.logging.LogLevel
import com.aallam.openai.api.logging.Logger
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.*
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
        given(userSettingsRepository.getSettings()).willReturn(UserSettings("", "", false, "", "", false, "", 1, "informal", "gpt-3.5-turbo-0613", true, 10))
        val key = System.getenv("openai_api_key")
        given(openAIConfigProvider.getInstance()).willReturn(
            OpenAI(
                OpenAIConfig(
                    token = key,
                    logging = LoggingConfig(LogLevel.All, Logger.Default),
                    timeout = Timeout(socket = 60.seconds),
                    organization = null,
                    headers = mapOf(),
                    host = OpenAIHost.OpenAI,
                    proxy = null,
                    retry = RetryStrategy(
                        base = 2.0,
                        maxRetries = 2,
                        maxDelay = 60.seconds,
                    )
                )
            )
        )
        parser = TranslationRequestResponseMapper(objectMapper)
        sut = GPTTranslationRequestClient(openAIConfigProvider, parser, dispatcherConfiguration, userSettingsRepository)
    }

    @Test
    fun testThatRequestWorks() {
        runBlocking {
            val baseContent = """
                key: "premium_type"
                value: "Your premium type is ${'$'}{appUser.premiumType} and you have ${'$'}{credits} Credits"
                description: "show user his premiumType and the credits (pluralize, int)"
            """.trimIndent()

            val it = sut.requestComplexTranslationLong(baseContent, "en")
            assertThat(it).isNotNull
            val value = objectMapper.readTree(it)
            assertThat(value.get("premium_type").textValue()).isEqualTo("Your premium type is {premiumType} and you have {credits, plural, =0{no Credits} =1{1 Credit} other{{credits} Credits}}");
            assertThat(value.path("@premium_type").path("description").textValue()).contains("show user his premiumType and the credits")
            assertThat(value.path("@premium_type").path("placeholders").path("premiumType").path("type").textValue()).isEqualTo("string")
            assertThat(value.path("@premium_type").path("placeholders").path("credits").path("type").textValue()).isEqualTo("num")

        }
    }
}
