package de.keeyzar.gpthelper.gpthelper.features.psiutils

import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.lang.dart.psi.DartAdditiveExpression
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression

class DartAdditiveExpressionExtractorTest : BasePlatformTestCase() {

    private lateinit var dartAdditiveExpressionExtractor: DartAdditiveExpressionExtractor

    override fun setUp() {
        super.setUp()
        dartAdditiveExpressionExtractor = DartAdditiveExpressionExtractor()
        myFixture.testDataPath = "src/test/resources"
    }

    fun testComplexString() {
        val psiFile = myFixture.configureByText("Test.dart", """
            void main() {
                String complexString = "hello" + 3 + "world";
            }
        """.trimIndent())

        val stringLiteral = psiFile.descendantsOfType<DartStringLiteralExpression>().first()
        val extractAdditiveExpression = dartAdditiveExpressionExtractor.extractAdditiveExpression(stringLiteral)?.text
        assertEquals("\"hello\" + 3 + \"world\"", extractAdditiveExpression)
    }


    fun testAdditiveExpression2() {
        val psiFile = myFixture.configureByFile("psiutils/dart/additive_expr_2.dart")

        val stringLiteral = psiFile.descendantsOfType<DartStringLiteralExpression>().first()
        val extractAdditiveExpression = dartAdditiveExpressionExtractor.extractAdditiveExpression(stringLiteral)?.text
        assertEquals("'nice ' + 'world ' + 'new'", extractAdditiveExpression)
    }

    fun testIsDescendantOf_true() {
        val psiFile = myFixture.configureByFile("psiutils/dart/additive_expr_2.dart")

        val stringLiteral = psiFile.descendantsOfType<DartStringLiteralExpression>().first()
        val additiveExpression = psiFile.descendantsOfType<DartAdditiveExpression>().first()

        assertTrue(dartAdditiveExpressionExtractor.isDescendantOf(stringLiteral, additiveExpression))
    }
}
