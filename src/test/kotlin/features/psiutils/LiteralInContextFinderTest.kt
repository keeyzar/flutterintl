package de.keeyzar.gpthelper.gpthelper.features.psiutils

import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression

class LiteralInContextFinderTest : BasePlatformTestCase() {
    private lateinit var sut: LiteralInContextFinder

    override fun setUp() {
        super.setUp()
        sut = LiteralInContextFinder()
        myFixture.testDataPath = "src/test/resources"
    }

    fun testFindLiteralInContext() {
        val psiFile = myFixture.configureByText("Test.dart", """
            void main() {
                String complexString = "hello" + 3 + "world";
            }
        """.trimIndent())

        val stringLiteral = psiFile.descendantsOfType<DartStringLiteralExpression>().first()

        val literalInContext = sut.findContext(stringLiteral)

        assertEquals("""String complexString = "hello" + 3 + "world"""", literalInContext.text)
    }
}
