//package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service
//
//import de.keeyzar.gpthelper.gpthelper.DIConfig.Companion.appModule
//import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserAddNewLanguageRequest
//import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslateStringRequest
//import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
//import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.TranslationService
//import kotlinx.coroutines.runBlocking
//import org.assertj.core.api.Assertions
//import org.junit.Before
//import org.junit.Ignore
//import org.junit.Test
//
//import org.koin.core.context.startKoin
//import org.koin.dsl.module
//import org.koin.test.KoinTest
//import org.koin.test.inject
//
//class GPTTranslationServiceTest : KoinTest {
//
//    private val gptTranslationService: TranslationService by inject()
//
//    @Before
//    fun setUp() {
//        startKoin {
//            modules(appModule, module {
//                single<UserSettingsRepository> { OpenAIUserSettingsRepositoryTestUtil() }
//            })
//        }
//    }
//
//    @Test
//    @Ignore("This is a costly test, only execute, when you want to test")
//    fun translate() {
//        var request = UserTranslateStringRequest("key", "value", "description", setOf("de", "en", "fr", "es_ES"))
//        runBlocking {
//            val response = gptTranslationService.translate(request);
//
//            Assertions.assertThat(response).hasSize(4)
//            response.map { it.lang }.forEach {
//                Assertions.assertThat(it).isIn("de", "en", "fr", "es_ES")
//            }
//        }
//    }
//
//    @Test
//    @Ignore("This is a costly test, only execute, when you want to test")
//    fun translateWholeFile() {
//        val request = UserAddNewLanguageRequest(exampleDummyJson, "de");
//        runBlocking {
//            val response = gptTranslationService.translateWholeFile(request);
//            Assertions.assertThat(response).isNotBlank()
//        }
//    }
//
//    private val exampleDummyJson = """
//        {
//  "helloWorld": "Hello World!",
//  "@helloWorld": {
//    "description": "The conventional newborn programmer greeting"
//  },
//  "main_page_title": "Holotropic Breathwork",
//  "@main_page_title": {
//    "description": "Title of the main page"
//  },
//  "main_page_cards_breathing_exercise_title": "Breathing Exercise",
//  "@main_page_cards_breathing_exercise_title": {
//    "description": "Title of the card that links to the breathing exercise"
//  },
//  "main_page_cards_breathing_exercise_subtitle": "like Wim Hof",
//  "@main_page_cards_breathing_exercise_subtitle": {
//    "description": "Description of the card that links to the breathing exercise"
//  }
//  }
//    """.trimIndent()
//}
