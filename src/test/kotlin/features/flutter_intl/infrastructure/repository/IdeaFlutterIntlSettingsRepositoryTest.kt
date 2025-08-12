//package de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.repository
//
//import com.fasterxml.jackson.databind.ObjectMapper
//import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
//import com.intellij.openapi.project.Project
//import com.intellij.openapi.util.Disposer
//import com.intellij.testFramework.LightProjectDescriptor
//import com.intellij.testFramework.fixtures.CodeInsightTestFixture
//import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
//import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
//import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.entity.FlutterIntlSettings
//import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.exceptions.FlutterIntlFileParseException
//import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository.FlutterIntlSettingsRepository
//import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
//import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.ObjectMapperProvider
//import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
//import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.CurrentProjectProvider
//import org.assertj.core.api.Assertions.assertThat
//import org.assertj.core.api.Assertions.assertThatCode
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import org.koin.core.context.loadKoinModules
//import org.koin.core.context.stopKoin
//import org.koin.dsl.module
//import org.koin.test.KoinTest
//import org.koin.test.inject
//import org.mockito.kotlin.*
//import java.nio.file.Path
//import java.nio.file.Paths
//
//class IdeaFlutterIntlSettingsRepositoryTest : KoinTest {
//
//    companion object {
//        private val nonDefaultFullArbExample = """
//            arb-dir: something/something
//            template-arb-file: appreal_en.arb
//            output-localization-file: fancy_name.dart
//            untranslated-messages-file: are_there_any_untranslated_messages.txt
//            output-class: TestTime
//            nullable-getter: false
//        """.trimIndent()
//        private val nonDefaultPartialArbExample = """
//            arb-dir: something/something
//            template-arb-file: appreal_en.arb
//            output-localization-file: fancy_name.dart
//            output-class: TestTime
//        """.trimIndent()
//
//        private val nonDefaultIntlSettings = FlutterIntlSettings(
//            arbDir = "something/something",
//            templateArbFile = "appreal_en.arb",
//            outputLocalizationFile = "fancy_name.dart",
//            untranslatedMessagesFile = "are_there_any_untranslated_messages.txt",
//            outputClass = "TestTime",
//            nullableGetter = false
//        )
//        private val nonDefaultPartialIntlSettings = FlutterIntlSettings().copy(
//            arbDir = "something/something",
//            templateArbFile = "appreal_en.arb",
//            outputLocalizationFile = "fancy_name.dart",
//            outputClass = "TestTime"
//        )
//
//        private val defaultUserSettings = UserSettings(
//            arbDir = "",
//            openAIKey = "test_key",
//            outputClass = "TestAppLocalizations",
//            nullableGetter = true,
//            templateArbFile = "test_template_arb_file_en.arb",
//            intlConfigFile = "test_intl_config_file.yaml",
//            watchIntlConfigFile = true,
//            outputLocalizationFile = "test_output_localization_file"
//        )
//    }
//
//    private val sut: FlutterIntlSettingsRepository by inject()
//    private val userSettingsRepository: UserSettingsRepository by inject()
//    private val currentProjectProvider: CurrentProjectProvider by inject()
//    private val flutterFileRepository: FlutterFileRepository by inject()
//
//    private lateinit var project: Project
//    private lateinit var fixture: CodeInsightTestFixture
//
//
//    @Before
//    fun setUp() {
//        //because post startup activity already loads koin
//        val factory = IdeaTestFixtureFactory.getFixtureFactory()
//        val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
//        val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "my_new_project")
//        val myFixture = fixtureBuilder.fixture
//        fixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
//            myFixture,
//            LightTempDirTestFixtureImpl(true)
//        )
//        fixture.setUp()
//        project = fixture.project
//
//        loadKoinModules(
//            module {
//                single<UserSettingsRepository> { mock<UserSettingsRepository>() }
//                single<CurrentProjectProvider> { mock<CurrentProjectProvider>() }
//                single<FlutterFileRepository> { mock<FlutterFileRepository>() }
//                single<ObjectMapper> { ObjectMapperProvider().provideObjectMapper(YAMLFactory()) }
//            }
//        )
//    }
//
//    @After
//    fun tearDown() {
//        try {
//            fixture.tearDown()
//            Disposer.dispose(fixture.testRootDisposable)
//        } catch (e: Throwable) {
//            e.printStackTrace()
//        } finally {
//
//        }
//    }
//
//    @Test
//    fun `should return default settings given empty flutter settings`() {
//        given(userSettingsRepository.getSettings()).willReturn(defaultUserSettings)
//        given(currentProjectProvider.project).willReturn(project)
//        given(flutterFileRepository.getFileContent(any(), any())).willReturn("")
//
//        val result = sut.getFlutterIntlSettings()
//
//        assertThat(result).isEqualTo(FlutterIntlSettings())
//    }
//
//
//    @Test
//    fun `should find correct file given userSettingsIntlFile`() {
//        val testSettings = defaultUserSettings.copy(intlConfigFile = "random.yaml")
//        given(userSettingsRepository.getSettings()).willReturn(testSettings)
//        given(currentProjectProvider.project).willReturn(project)
//        given(flutterFileRepository.getFileContent(any(), any())).willReturn("")
//
//        val result = sut.getFlutterIntlSettings()
//
//        assertThat(result).isEqualTo(FlutterIntlSettings())
//        verify(flutterFileRepository).getFileContent(any(), eq(Path.of("${project.basePath}/random.yaml")))
//    }
//
//    @Test
//    fun `should parse all the content from the settings file`() {
//        given(userSettingsRepository.getSettings()).willReturn(defaultUserSettings)
//        given(currentProjectProvider.project).willReturn(project)
//        given(flutterFileRepository.getFileContent(any(), any())).willReturn(nonDefaultFullArbExample)
//
//        val result = sut.getFlutterIntlSettings()
//
//        assertThat(result).isEqualTo(nonDefaultIntlSettings)
//    }
//
//    @Test
//    fun `should parse partial content from the settings file`() {
//        given(userSettingsRepository.getSettings()).willReturn(defaultUserSettings)
//        given(currentProjectProvider.project).willReturn(project)
//        given(flutterFileRepository.getFileContent(any(), any())).willReturn(nonDefaultPartialArbExample)
//
//        val result = sut.getFlutterIntlSettings()
//
//        assertThat(result).isEqualTo(nonDefaultPartialIntlSettings)
//    }
//
//    @Test
//    fun `should use provided`() {
//        given(currentProjectProvider.project).willReturn(project)
//        given(flutterFileRepository.getFileContent(any(), any()))
//            .willReturn("")
//
//        sut.loadFlutterIntlSettingsByPath(Paths.get("/something/test.yaml"))
//
//        verify(flutterFileRepository).getFileContent(any(), eq(Paths.get("/something/test.yaml")))
//    }
//
//    @Test
//    fun `should throw exception, when invalid yaml`() {
//        given(currentProjectProvider.project).willReturn(project)
//        given(flutterFileRepository.getFileContent(any(), any()))
//            .willReturn("does it work?")
//
//        assertThatCode {
//            sut.loadFlutterIntlSettingsByPath(Paths.get("/something/test.yaml"))
//        }.isInstanceOf(FlutterIntlFileParseException::class.java)
//    }
//}
