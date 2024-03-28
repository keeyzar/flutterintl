package de.keeyzar.gpthelper.gpthelper.features.changetranslation.infrastructure.psi

import com.intellij.json.psi.JsonStringLiteral
import com.intellij.json.psi.impl.JsonPropertyImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.descendantsOfType
import com.intellij.psi.util.parentOfType
import com.intellij.psi.util.siblings
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

    fun test_givenWrongKey_shouldStillFindCorrectKeyAndValue() {
        fun bracketPsi(psiFile: PsiFile): PsiElement {
            return psiFile.descendantsOfType<PsiElement>()
                .find { it.text == "}" }!!
        }

        fun firstElement(psiFile: PsiFile): PsiElement {
            return psiFile.descendantsOfType<JsonStringLiteral>().first()
        }

        fun lastElement(psiFile: PsiFile): PsiElement {
            return psiFile.descendantsOfType<JsonPropertyImpl>()
                .take(2)
                .last()
        }

        fun lastElementString(psiFile: PsiFile): PsiElement {
            return psiFile.descendantsOfType<JsonStringLiteral>()
                .last()
        }

        /**
         * check if we find hte correct element, when we're in a nested whitespace
         */
        fun nestedWhitespace(psiFile: PsiFile): PsiElement {
            return psiFile.descendantsOfType<LeafPsiElement>()
                .find { it.textMatches("\"greet the user\"") }!!
                .parentOfType<JsonPropertyImpl>()?.prevSibling!!
        }

        fun commaEndOfLine(psiFile: PsiFile): PsiElement {
            return psiFile.descendantsOfType<LeafPsiElement>()
                .find { it.textMatches(",") }!!
        }

        val listOfFunctions = listOf(::bracketPsi,
            ::firstElement,
            ::lastElement,
            ::lastElementString,
            ::nestedWhitespace,
            ::commaEndOfLine
        )

        val psiFile = myFixture.configureByText(
            "test.arb", """
            {
                "greeting": "hello user"
                "@greeting": {
                    "type": "text",
                    "description": "greet the user"
                }
            }
        """.trimIndent()
        )


        //for each function in the list, we call it with the psiFile as argument and check if the key and value are correct
        listOfFunctions.forEach {
            println("function name: ${it.name}")
            val firstElement = it(psiFile)
            val currentJsonProperty = arbPsiUtils.getCurrentJsonProperty(firstElement)

            assertThat(currentJsonProperty?.key).isEqualTo("greeting")
            assertThat(currentJsonProperty?.value).isEqualTo("hello user")
            assertThat(currentJsonProperty?.description).isEqualTo("greet the user")
        }
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