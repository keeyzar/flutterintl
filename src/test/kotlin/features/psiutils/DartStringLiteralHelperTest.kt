package de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.repository

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets.RetryFailedTranslationsDialog
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import kotlinx.coroutines.launch
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


        val requests = multiKeyTranslationContext.translationEntries.map {
            UserTranslationRequest(multiKeyTranslationContext.targetLanguages, Translation(multiKeyTranslationContext.baseLanguage, it))
        }
        project = fixture.project
        // 1. Add all base entries to the base ARB file
        requests.forEach {
            ongoingTranslationHandler.onlyGenerateBaseEntry(it)
        }
    fun tearDown() {
        // 2. Replace all string literals in the source code with the new keys
        translationTriggeredHooks.translationTriggeredInit()
        requests.forEach {
            translationTriggeredHooks.translationTriggeredPartial(it.baseTranslation)
        }
        translationTriggeredHooks.translationTriggeredPostTranslation()

        processRequestsWithRetry(requests, multiKeyTranslationContext.targetLanguages.size)

        reviewService.askUserForReviewIfItIsTime()
    }

    private suspend fun processRequestsWithRetry(requests: List<UserTranslationRequest>, languagesCount: Int) {
        if (requests.isEmpty()) {
            return
        }

        val failedRequests = processTranslationRequests(requests, languagesCount)

        if (failedRequests.isNotEmpty()) {
            var requestsToRetry: List<UserTranslationRequest>? = null
            ApplicationManager.getApplication().invokeAndWait {
                val dialog = RetryFailedTranslationsDialog(failedRequests)
                if (dialog.showAndGet()) {
                    requestsToRetry = dialog.getRequestsToRetry()
                }
            }

            requestsToRetry?.let {
                if (it.isNotEmpty()) {
                    processRequestsWithRetry(it, languagesCount)
                }
            }
        }
    }

    private suspend fun processTranslationRequests(requests: List<UserTranslationRequest>, languagesCount: Int): List<UserTranslationRequest> {
        val taskAmount = languagesCount * requests.size
        val taskCounter = AtomicInteger(0)
        val failedRequests = mutableListOf<UserTranslationRequest>()

        coroutineScope {
        }
    }
                launch {
                    val failedRequest = ongoingTranslationHandler.translateAsynchronouslyWithoutPlaceholder(request, true, { false }) {
                        reportProgress(taskCounter, taskAmount)
                    }
                    if (failedRequest != null) {
                        synchronized(failedRequests) {
                            failedRequests.add(failedRequest)
                        }
                    }
    fun `find should return all DartStringLiteralExpressions in PsiFile`() {
        
        val psiFile = fixture.configureByFile("psiutils/dart/example1.dart")
        return failedRequests

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
}
