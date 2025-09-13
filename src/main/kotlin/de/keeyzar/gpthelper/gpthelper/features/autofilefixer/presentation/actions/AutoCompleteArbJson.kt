package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search.ArbFileContentProvider
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search.ArbSuggestionService
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.ImportFixer
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.StatementFixer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject


class AutoCompleteArbJson : CompletionContributor(), KoinComponent {

    // DI - translation repository & arb file service are provided by Koin
    private val translationFileRepository: TranslationFileRepository by inject()
    private val arbFilesService: ArbFilesService by inject()
    private val importFixer: ImportFixer by inject()
    private val statementFixer: StatementFixer by inject()

    private val suggestionService by lazy {
        ArbSuggestionService(
            ArbFileContentProvider(
                arbFilesService,
                translationFileRepository
            )
        )
    }

    init {
        // refresh index eagerly
        try {
            suggestionService.refreshIndexIfNeeded()
        } catch (_: Exception) {
            // ignore - indexing will be attempted again on demand
        }
        val provider = object : CompletionProvider<CompletionParameters>() {
            override fun addCompletions(
                parameters: CompletionParameters,
                context: ProcessingContext,
                result: CompletionResultSet
            ) {
                val prefix = result.prefixMatcher.prefix
                if (prefix.isBlank()) return

                try {
                    // ensure up-to-date index
                    suggestionService.refreshIndexIfNeeded()
                    val suggestions = suggestionService.suggest(prefix, 10)
                    for (s in suggestions) {
                        val lookup = LookupElementBuilder.create(s.key)
                            .withTypeText(s.value, true)
                            .withInsertHandler { insertionContext, item ->
                                val project = insertionContext.project
                                val editor = insertionContext.editor
                                val document = editor.document
                                val tailOffset = insertionContext.tailOffset
                                //we need to calculate the start offset ourselves, because the one from the context is wrong
                                val startOffset = tailOffset - item.lookupString.length
                                val element =
                                    insertionContext.file.findElementAt(startOffset) ?: return@withInsertHandler

                                // 1. Find the 'const' keyword to remove, up to 3 levels up.
                                val constElementToRemove = run {
                                    var p = element.parent
                                    for (i in 0..5) {
                                        if (p == null) break
                                        p.node.findChildByType(DartTokenTypes.CONST)?.psi?.let { return@run it }
                                        p = p.parent
                                    }
                                    null
                                }

                                // 2. Remove the 'const' keyword and the space after it.
                                var removedLength = 0
                                if (constElementToRemove != null) {
                                    val constStartOffset = constElementToRemove.textRange.startOffset
                                    val nextElement = PsiTreeUtil.nextLeaf(constElementToRemove)
                                    val constEndOffset = if (nextElement != null && nextElement.text.isBlank()) {
                                        nextElement.textRange.endOffset
                                    } else {
                                        constElementToRemove.textRange.endOffset
                                    }

                                    if (constStartOffset < startOffset) {
                                        document.deleteString(constStartOffset, constEndOffset)
                                        removedLength = constEndOffset - constStartOffset
                                    }
                                }

                                // 3. Adjust offsets and find the current element again.
                                val adjustedStartOffset = startOffset - removedLength
                                val currentElement = insertionContext.file.findElementAt(adjustedStartOffset) ?: element
                                val newStatement = statementFixer.createStatement(currentElement, item.lookupString)
                                val stringLiteral =
                                    PsiTreeUtil.getParentOfType(currentElement, DartStringLiteralExpression::class.java)

                                if (stringLiteral != null) {
                                    val stringStartOffset = stringLiteral.textRange.startOffset
                                    val stringEndOffset = stringLiteral.textRange.endOffset
                                    document.replaceString(stringStartOffset, stringEndOffset, newStatement)
                                    editor.caretModel.moveToOffset(stringStartOffset + newStatement.length)
                                } else {
                                    // Replace the typed prefix with the full statement
                                    val adjustedTailOffset = tailOffset - removedLength
                                    document.replaceString(adjustedStartOffset, adjustedTailOffset, newStatement)
                                    editor.caretModel.moveToOffset(adjustedStartOffset + newStatement.length)
                                }
                                PsiDocumentManager.getInstance(project).commitDocument(document)
                                val newElement = insertionContext.file.findElementAt(editor.caretModel.offset - 1) ?: return@withInsertHandler
                                importFixer.addTranslationImportIfMissing(project, newElement)
                            }
                        result.addElement(lookup)
                    }
                } catch (e: Exception) {
                    // on any failure just don't add suggestions
                }
            }
        }


        extend(
            CompletionType.BASIC,
            psiElement().with(DartStringRequiredPattern()),
            provider
        )
    }
}