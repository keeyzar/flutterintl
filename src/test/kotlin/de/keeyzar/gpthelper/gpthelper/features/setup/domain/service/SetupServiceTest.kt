//package de.keeyzar.gpthelper.gpthelper.features.setup.domain.service
//
//import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.InstallFileProvider
//import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.PathProvider
//import org.junit.jupiter.api.Assertions.*
//import org.junit.jupiter.api.BeforeEach
//import org.junit.jupiter.api.Test
//import org.mockito.kotlin.*
//
//class SetupServiceTest {
//
//    private lateinit var setupService: SetupService
//    private val pathProvider: PathProvider = mock()
//    private val userInstallDialogs: UserInstallDialogs = mock()
//    private val fileProvider: InstallFileProvider = mock()
//    private val appReferenceProvider: AppReferenceProvider = mock()
//
//    @BeforeEach
//    fun setUp() {
//        setupService = SetupService(pathProvider, userInstallDialogs, fileProvider, appReferenceProvider)
//    }
//
//    @Test
//    fun `orchestrate should run full setup when nothing is configured`() {
//        // Given
//        whenever(pathProvider.getRootPath()).thenReturn("/dummy/path")
//        whenever(fileProvider.readFile(any())).thenReturn("name: my_flutter_app")
//        whenever(userInstallDialogs.showDiff(any(), any(), any())).thenReturn(true)
//        whenever(userInstallDialogs.confirmL10nConfiguration(any())).thenReturn(true)
//        whenever(appReferenceProvider.findAppReferences()).thenReturn(listOf(Any()) as List<Object>?)
//        whenever(appReferenceProvider.enableLocalization(any())).thenReturn(true)
//
//        // When
//        setupService.orchestrate()
//
//        // Then
//        verify(fileProvider, times(3)).readFile(any())
//        verify(userInstallDialogs).showDiff(eq("Install Localization Libraries"), any(), any())
//        verify(userInstallDialogs).confirmL10nConfiguration(any())
//        verify(appReferenceProvider).findAppReferences()
//        verify(appReferenceProvider).enableLocalization(any())
//    }
//
//    @Test
//    fun `orchestrate should abort when user declines dependency installation`() {
//        // Given
//        whenever(pathProvider.getRootPath()).thenReturn("/dummy/path")
//        whenever(fileProvider.readFile(any())).thenReturn("name: my_flutter_app")
//        whenever(userInstallDialogs.showDiff(any(), any(), any())).thenReturn(false)
//
//        // When
//        setupService.orchestrate()
//
//        // Then
//        verify(userInstallDialogs).showDiff(eq("Install Localization Libraries"), any(), any())
//        verify(userInstallDialogs, never()).confirmL10nConfiguration(any())
//        verify(appReferenceProvider, never()).findAppReferences()
//    }
//}