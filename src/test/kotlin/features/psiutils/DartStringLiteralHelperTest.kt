package de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.repository

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import de.keeyzar.gpthelper.gpthelper.features.psiutils.DartAdditiveExpressionExtractor
import de.keeyzar.gpthelper.gpthelper.features.psiutils.DartStringLiteralHelper
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

class DartStringLiteralHelperTest {

    private val sut: DartStringLiteralHelper = DartStringLiteralHelper(DartAdditiveExpressionExtractor(), emptyList())

    private lateinit var project: Project
    private lateinit var fixture: CodeInsightTestFixture


    @Before
    fun setUp() {
        //because post startup activity already loads koin
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val projectDescriptor = LightProjectDescriptor.EMPTY_PROJECT_DESCRIPTOR
        val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "my_new_project")
        val myFixture = fixtureBuilder.fixture
        fixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
            myFixture,
            LightTempDirTestFixtureImpl(true)
        )
        fixture.setUp()
        project = fixture.project
        fixture.testDataPath = "src/test/resources"
    }

    @After
    fun tearDown() {
        try {
            fixture.tearDown()
            Disposer.dispose(fixture.testRootDisposable)
        } catch (e: Throwable) {
            e.printStackTrace()
        } finally {

        }
    }

    @Test
    fun `find should return all DartStringLiteralExpressions in PsiFile`() {
        
        val psiFile = fixture.configureByFile("psiutils/dart/example1.dart")

        val dartStringLiterals = ReadAction.compute<List<DartStringLiteralExpression>, Throwable> {
            sut.findLiterals(psiFile)
        }

        assertThat(dartStringLiterals).hasSize(2)
    }


    @Test
    fun `find should return all DartStringLiteralExpressions in PsiFile example1`() {
        
        val psiFile = fixture.configureByFile("psiutils/dart/example1.dart")

        val dartStringLiterals = ReadAction.compute<List<DartStringLiteralExpression>, Throwable> {
            sut.findLiterals(psiFile)
        }

        assertThat(dartStringLiterals).hasSize(2)
    }

    @Test
    fun `find should return all DartStringLiteralExpressions in PsiFile example2`() {
        
        val psiFile = fixture.configureByFile("psiutils/dart/example2.dart")

        val dartStringLiterals = ReadAction.compute<List<DartStringLiteralExpression>, Throwable> {
            sut.findLiterals(psiFile)
        }

        assertThat(dartStringLiterals).hasSize(3)
    }

    @Test
    fun `find should return all DartStringLiteralExpressions in PsiFile example3`() {
        
        val psiFile = fixture.configureByFile("psiutils/dart/example3.dart")

        val dartStringLiterals = ReadAction.compute<List<DartStringLiteralExpression>, Throwable> {
            sut.findLiterals(psiFile)
        }

        assertThat(dartStringLiterals).hasSize(3)
    }

    @Test
    fun `find should return empty list for PsiFile without DartStringLiteralExpressions`() {
        
        val psiFile = fixture.configureByFile("psiutils/dart/empty.dart")

        val dartStringLiterals = ReadAction.compute<List<DartStringLiteralExpression>, Throwable> {
            sut.findLiterals(psiFile)
        }

        assertThat(dartStringLiterals).isEmpty()
    }

    @Test
    fun `find should return DartStringLiteralExpressions with correct text`() {
        
        val psiFile = fixture.configureByFile("psiutils/dart/example1.dart")

        val dartStringLiterals = ReadAction.compute<List<String>, Throwable> {
            sut.findLiterals(psiFile).map { it.text }
        }

        assertThat(dartStringLiterals).containsExactly("'Hello, World!'", "\"This is an example\"")
    }

    @Test
    fun `find should return DartStringLiteralExpressions with complex string`() {
        
        val psiFile = fixture.configureByFile("psiutils/dart/example4.dart")

        val dartStringLiterals = ReadAction.compute<List<String>, Throwable> {
            sut.findLiterals(psiFile).map { it.text }
        }

        assertThat(dartStringLiterals).containsExactly("'Hello, World!'", "\"This is an example\"", "\"nice\"")
    }
    @Test
    fun `find should return DartStringLiteralExpressions with complex string 2`() {
        
        val psiFile = fixture.configureByFile("psiutils/dart/example5.dart")

        val dartStringLiterals = ReadAction.compute<List<String>, Throwable> {
            sut.findLiterals(psiFile).map { it.text }
        }

        assertThat(dartStringLiterals).containsExactly("'Hello '", "' World!'")
    }

    @Test
    fun `find should not return DartStringLiteralExpressions from comments`() {
        
        val psiFile = fixture.configureByFile("psiutils/dart/with_comments.dart")

        val dartStringLiterals = ReadAction.compute<List<DartStringLiteralExpression>, Throwable> {
            sut.findLiterals(psiFile)
        }

        assertThat(dartStringLiterals).isEmpty()
    }


    @Test
    fun `find should return string only once, when multiple strings`() {

        val psiFile = fixture.configureByFile("psiutils/dart/additive_expr_3.dart")

        ReadAction.run<Throwable> {
            val elements = sut.findStringPsiElements(psiFile)
            assertThat(elements).hasSize(1)
        }
    }

    @Test
    fun `findStringPsiElements should mark print statements as not selected`() {
        val psiFile = fixture.configureByFile("psiutils/dart/example6.dart")

        ReadAction.run<Throwable> {
            val elementsWithSelection = sut.findStringPsiElements(psiFile)
            assertThat(elementsWithSelection).hasSize(2)

            val omgElement = elementsWithSelection.keys.find { it.text == "\"Omg\"" }
            assertThat(omgElement).isNotNull
            assertThat(elementsWithSelection[omgElement]).isFalse()

            val complexStringElement = elementsWithSelection.keys.find { it.text.startsWith("'Hello '") }
            assertThat(complexStringElement).isNotNull
            assertThat(elementsWithSelection[complexStringElement]).isTrue()
        }
    }
}
