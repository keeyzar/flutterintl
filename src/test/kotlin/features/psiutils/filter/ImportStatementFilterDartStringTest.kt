package de.keeyzar.gpthelper.gpthelper.features.psiutils.filter

import com.intellij.psi.util.descendantsOfType
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression

class ImportStatementFilterDartStringTest : BasePlatformTestCase() {

    private lateinit var sut: ImportStatementFilterDartString

    override fun setUp() {
        super.setUp()
        sut = ImportStatementFilterDartString()
        myFixture.testDataPath = "src/test/resources/psiutils/dart/filter"
    }

    fun testShouldFilterImportStatements() {
        val psiFile = myFixture.configureByFile("import_string_literal.dart")
        val stringLiteral = psiFile.descendantsOfType<DartStringLiteralExpression>().first()

        val filter = sut.filter(stringLiteral)

        assertFalse(filter)
    }
}
