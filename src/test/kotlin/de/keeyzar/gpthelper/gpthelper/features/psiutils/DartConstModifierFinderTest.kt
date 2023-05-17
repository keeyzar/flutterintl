package de.keeyzar.gpthelper.gpthelper.features.psiutils

import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression

class DartConstModifierFinderTest : BasePlatformTestCase() {
    private lateinit var sut: DartConstModifierFinder
    override fun setUp() {
        super.setUp()
        sut = DartConstModifierFinder()
        myFixture.testDataPath = "src/test/resources/psiutils/dart/const_modifier_finder"
    }

    fun testCheckForConstExpressionsInHierarchy_1() {
        val psiFile = myFixture.configureByFile("var_declaration_1.dart")
        val stringLiteral = psiFile.descendantsOfType<DartStringLiteralExpression>().first()

        val constModifier = sut.checkForConstExpressionsInHierarchy(stringLiteral)

        assertEquals("const", constModifier?.text)
    }

    fun testCheckForConstExpressionsInHierarchy_2() {
        val psiFile = myFixture.configureByFile("list_declaration_1.dart")
        val stringLiteral = psiFile.descendantsOfType<DartStringLiteralExpression>().first()

        val constModifier = sut.checkForConstExpressionsInHierarchy(stringLiteral)

        assertEquals("const", constModifier?.text)
    }

    fun testCheckForConstExpressionsInHierarchy_3() {
        val psiFile = myFixture.configureByFile("class_declaration_1.dart")
        val stringLiteral = psiFile.descendantsOfType<DartStringLiteralExpression>().first()

        val constModifier = sut.checkForConstExpressionsInHierarchy(stringLiteral)

        assertEquals("const", constModifier?.text)
    }


    fun testCheckForConstExpressionsInHierarchy_4() {
        val psiFile = myFixture.configureByFile("class_declaration_2.dart")
        val stringLiteral = psiFile.descendantsOfType<DartStringLiteralExpression>().first()

        val constModifier = sut.checkForConstExpressionsInHierarchy(stringLiteral)

        assertEquals("const", constModifier?.text)
    }
}

