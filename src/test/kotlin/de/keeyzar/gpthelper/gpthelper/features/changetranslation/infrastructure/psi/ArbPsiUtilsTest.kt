package de.keeyzar.gpthelper.gpthelper.features.changetranslation.infrastructure.psi

import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.impl.JsonPropertyImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.descendantsOfType
import com.intellij.psi.util.parentOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import de.keeyzar.gpthelper.gpthelper.features.psiutils.arb.ArbPsiUtils
import de.keeyzar.gpthelper.gpthelper.features.psiutils.arb.StringArrayContent
import org.assertj.core.api.Assertions.*

class ArbPsiUtilsTest : BasePlatformTestCase() {
    private lateinit var arbPsiUtils: ArbPsiUtils

    override fun setUp() {
        super.setUp()
        arbPsiUtils = ArbPsiUtils()
        myFixture.testDataPath = "src/test/resources"
    }

    fun test_bracketPsi_shouldFindCorrectKeyAndValue() {
        val psiFile = createPsiFile()
        val element = psiFile.descendantsOfType<PsiElement>()
            .find { it.text == "}" }!!
        val currentJsonProperty = arbPsiUtils.getCurrentJsonProperty(element)

        assertThat(currentJsonProperty?.key).isEqualTo("greeting")
        assertThat(currentJsonProperty?.value).isEqualTo("hello user")
        assertThat(currentJsonProperty?.description).isEqualTo("greet the user")
    }

    fun test_firstElement_shouldFindCorrectKeyAndValue() {
        val psiFile = createPsiFile()
        val element = psiFile.descendantsOfType<JsonStringLiteral>().first()
        val currentJsonProperty = arbPsiUtils.getCurrentJsonProperty(element)

        assertThat(currentJsonProperty?.key).isEqualTo("greeting")
        assertThat(currentJsonProperty?.value).isEqualTo("hello user")
        assertThat(currentJsonProperty?.description).isEqualTo("greet the user")
    }

    fun test_lastElement_shouldFindCorrectKeyAndValue() {
        val psiFile = createPsiFile()
        val element = psiFile.descendantsOfType<JsonPropertyImpl>()
            .take(2)
            .last()
        val currentJsonProperty = arbPsiUtils.getCurrentJsonProperty(element)

        assertThat(currentJsonProperty?.key).isEqualTo("greeting")
        assertThat(currentJsonProperty?.value).isEqualTo("hello user")
        assertThat(currentJsonProperty?.description).isEqualTo("greet the user")
    }

    fun test_lastElementString_shouldFindCorrectKeyAndValue() {
        val psiFile = createPsiFile()
        val element = psiFile.descendantsOfType<JsonStringLiteral>()
            .find { it -> it.textMatches("\"hello user\"")}
        val currentJsonProperty = arbPsiUtils.getCurrentJsonProperty(element!!)

        assertThat(currentJsonProperty?.key).isEqualTo("greeting")
        assertThat(currentJsonProperty?.value).isEqualTo("hello user")
        assertThat(currentJsonProperty?.description).isEqualTo("greet the user")
    }

    fun test_nestedWhitespace_shouldFindCorrectKeyAndValue() {
        val psiFile = createPsiFile()
        val element = psiFile.descendantsOfType<LeafPsiElement>()
            .find { it.textMatches("\"greet the user\"") }!!
            .parentOfType<JsonPropertyImpl>()?.prevSibling!!
        val currentJsonProperty = arbPsiUtils.getCurrentJsonProperty(element)

        assertThat(currentJsonProperty?.key).isEqualTo("greeting")
        assertThat(currentJsonProperty?.value).isEqualTo("hello user")
        assertThat(currentJsonProperty?.description).isEqualTo("greet the user")
    }
    fun test_centerEntry_shouldFindCorrectKeyAndValue() {
        val psiFile = createPsiFile()
        val element = psiFile.descendantsOfType<JsonStringLiteral>()
            .find { it.textMatches("\"say hello\"") }!!
        val currentJsonProperty = arbPsiUtils.getCurrentJsonProperty(element)

        assertThat(currentJsonProperty?.key).isEqualTo("say_hello")
        assertThat(currentJsonProperty?.value).isEqualTo("nice")
        assertThat(currentJsonProperty?.description).isEqualTo("say hello")
    }

    fun test_commaEndOfLine_shouldFindCorrectKeyAndValue() {
        val psiFile = createPsiFile()
        val element = commaEndOfLine(psiFile)
        val currentJsonProperty = arbPsiUtils.getCurrentJsonProperty(element)

        assertThat(currentJsonProperty?.key).isEqualTo("greeting")
        assertThat(currentJsonProperty?.value).isEqualTo("hello user")
        assertThat(currentJsonProperty?.description).isEqualTo("greet the user")
    }

    private fun createPsiFile(): PsiFile {
        return myFixture.configureByText(
            "test.arb", """
            {
                "greeting": "hello user"
                "@greeting": {
                    "type": "text",
                    "description": "greet the user"
                }
                "say_hello": "nice",
                "@say_hello": {
                    "type": "text",
                    "description": "say hello"
                }
                "third": "another",
                "@third": {
                    "type": "text",
                    "description": "else"
                }
            }
        """.trimIndent()
        )
    }

    private fun commaEndOfLine(psiFile: PsiFile): PsiElement {
        return psiFile.descendantsOfType<LeafPsiElement>()
            .find { it.textMatches(",") }!!
    }

    fun test_givenUntranslatedMessageFileAllEntriesAreFound_SingleLanguage(){
        val psiFile = myFixture.configureByText(
            "untranslated_messages.txt", """
            {
                "de": ["greeting", "doesWork"]
            }
        """.trimIndent()
        )

        val allEntries = arbPsiUtils.getAllStringArraysFromRoot(psiFile)

        assertThat(allEntries).hasSize(1)
        assertThat(allEntries[0].key).isEqualTo("de")
        assertThat(allEntries[0].values).containsExactly("greeting", "doesWork")
    }


    fun test_givenUntranslatedMessageFileAllEntriesAreFound_MultipleLanguages(){
        val psiFile = myFixture.configureByText(
            "untranslated_messages.txt", """
            {
                "de": ["greeting", "doesWork"],
                "en": ["greeting"]
            }
        """.trimIndent()
        )

        val allEntries: List<StringArrayContent> = arbPsiUtils.getAllStringArraysFromRoot(psiFile)

        assertThat(allEntries).hasSize(2)
        assertThat(allEntries.map { it.key }).containsExactly("de", "en")
        assertThat(allEntries.map { it.values }).containsExactly(listOf("greeting", "doesWork"), listOf("greeting"))
    }

}