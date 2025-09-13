package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import com.intellij.lang.Language
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil // <-- **REQUIRED IMPORT**
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression // <-- **REQUIRED IMPORT**
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.entity.FlutterIntlSettings
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository.FlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.FileToTranslate
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import org.junit.Before
import org.junit.jupiter.api.Assertions.*
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.any

class ExistingKeyFinderTest : BasePlatformTestCase() {

    @Mock
    private lateinit var translationFileRepository: TranslationFileRepository
    @Mock
    private lateinit var arbFilesService: ArbFilesService
    @Mock
    private lateinit var settingsRepository: FlutterIntlSettingsRepository
    private lateinit var existingKeyFinder: ExistingKeyFinder

    override fun setUp() {
        super.setUp()
        MockitoAnnotations.openMocks(this)
        existingKeyFinder = ExistingKeyFinder(translationFileRepository, arbFilesService)
    }

    fun testFindExistingKeys() {
        // --- GIVEN ---
        val settings = FlutterIntlSettings(
            arbDir = "lib/l10n",
            templateArbFile = "app_en.arb",
            outputLocalizationFile = "l10n.dart"
        )
        `when`(settingsRepository.getFlutterIntlSettings()).thenReturn(settings)

        val arbContent = """
            {
              "common_save": "Save",
              "common_cancel": "Cancel",
              "user_profile_title": "User Profile",
              "error_network": "nerwork error",
              "action_delete": "delete",
              "@action_delete": {
                "description": "A button to delete an item"
              }
            }
        """.trimIndent()
        val lang =
            de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language.fromISOLangString("en_EN")
        `when`(arbFilesService.getBaseLanguage(null))
            .thenReturn(lang)
        `when`(translationFileRepository.getTranslationFileByLanguage(any()))
            .thenReturn(FileToTranslate(lang, arbContent))
        WriteCommandAction.runWriteCommandAction(project) {
            val arbDir = myFixture.tempDirFixture.findOrCreateDir("lib/l10n")
            val arbFile = arbDir.createChildData(this, settings.templateArbFile)
            arbFile.setBinaryContent(arbContent.toByteArray())
        }

        val psiElementsToTest = listOf(
            createDartStringLiteral("'Save'"), // Exact match
            createDartStringLiteral("'cancel'"), // Case-insensitive match
            createDartStringLiteral("\"Usr Frofile\""), // Fuzzy match (distance 2)
            createDartStringLiteral("'deletee'"), // Fuzzy match (distance 1)
            createDartStringLiteral("'network error'"), // Fuzzy match for a value with a typo in the arb file
            createDartStringLiteral("'NonExistent'"), // No match
            createDartStringLiteral("'A button to delete an item'") // Should not match description
        )

        // --- WHEN ---
        val result = existingKeyFinder.findExistingKeys(psiElementsToTest)

        // --- THEN ---
        assertNotNull(result)
        assertEquals(5, result.size)

        fun getKeyForText(text: String): String? {
            return result.entries.find { it.key.text == text }?.value
        }

        assertEquals("common_save", getKeyForText("'Save'"))
        assertEquals("common_cancel", getKeyForText("'cancel'"))
        assertEquals("user_profile_title", getKeyForText("\"Usr Frofile\""))
        assertEquals("action_delete", getKeyForText("'deletee'"))
        assertEquals("error_network", getKeyForText("'network error'"))
        assertNull(getKeyForText("'NonExistent'"))
        assertNull(getKeyForText("'A button to delete an item'"))
    }

    /**
     * Helper function to create a PsiElement for a Dart string literal.
     * This is the corrected version.
     */
    private fun createDartStringLiteral(text: String): PsiElement {
        val file = PsiFileFactory.getInstance(project).createFileFromText(
            "dummy.dart",
            Language.findLanguageByID("Dart")!!,
            "var a = $text;"
        )
        val literalExpression = PsiTreeUtil.findChildOfType(file, DartStringLiteralExpression::class.java)

        // Add a helpful assertion for debugging the test itself
        assertNotNull("Failed to create DartStringLiteralExpression for text: $text. The PSI tree might not have been parsed as expected.", literalExpression)

        return literalExpression!!
    }

    override fun tearDown() {
        super.tearDown()
    }
}