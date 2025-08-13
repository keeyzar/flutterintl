package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.genai.Client
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.LLMConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.TranslationRequestResponseMapper
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.given
import java.util.concurrent.Executors

class GPTTranslationRequestClientTest {
    @Mock
    private lateinit var LLMConfigProvider: LLMConfigProvider
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
        given(userSettingsRepository.getSettings()).willReturn(
            UserSettings(
                "",
                "",
                false,
                "",
                "",
                false,
                "",
                1,
                "informal",
                "gpt-3.5-turbo-0613",
                true,
                10
            )
        )
        val key = System.getenv("gemini_api_key")
        given(LLMConfigProvider.getInstanceGemini()).willReturn(
            Client.builder()
                .apiKey(key)
                .build()
        )
        parser = TranslationRequestResponseMapper(objectMapper)
        sut = GPTTranslationRequestClient(LLMConfigProvider, parser, dispatcherConfiguration, userSettingsRepository)
    }

    @Test
    fun testThatRequestWorks() {
        runBlocking {
//            val baseContent = """
//                key: "premium_type"
//                value: "Your premium type is ${'$'}{appUser.premiumType} and you have ${'$'}{credits} Credits"
//                description: "show user his premiumType and the credits (pluralize, int)"
//            """.trimIndent()

            val baseContent = """
                key: "rhythmtrainer_display_bpm"
                value: "'BPM: ${'$'}{_bpm.round()}'"
                description: "Label for displaying beats per minute with dynamic value"
            """.trimIndent()

            val it = sut.requestComplexTranslationLong(baseContent, "en")
            println(it);
            assertThat(it).isNotNull
//            val value = objectMapper.readTree(it)
//            assertThat(
//                value.get("premium_type").textValue()
//            ).contains(" =0{no credits} =1{1 credit} other{{credits} credits}}")
//            assertThat(
//                value.path("@premium_type").path("description").textValue()
//            ).contains("show user his premiumType and the credits")
//            assertThat(
//                value.path("@premium_type").path("placeholders").path("premiumType").path("type").textValue()
//            ).isEqualTo("string")
//            assertThat(
//                value.path("@premium_type").path("placeholders").path("credits").path("type").textValue()
//            ).isEqualTo("num")

        }
    }
}
