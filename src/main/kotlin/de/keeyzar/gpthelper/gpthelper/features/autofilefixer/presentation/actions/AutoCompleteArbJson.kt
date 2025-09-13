package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.lang.Language
import com.intellij.patterns.PlatformPatterns
import com.intellij.util.ProcessingContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search.ArbSuggestionService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.removeQuotes
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search.ArbFileContentProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.ImportFixer
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.StatementFixer


class AutoCompleteArbJson : CompletionContributor(), KoinComponent {

    // DI - translation repository & arb file service are provided by Koin
    private val translationFileRepository: TranslationFileRepository by inject()
    private val arbFilesService: ArbFilesService by inject()
    private val importFixer: ImportFixer by inject()
    private val statementFixer: StatementFixer by inject()

    private val suggestionService by lazy { ArbSuggestionService(ArbFileContentProvider(arbFilesService, translationFileRepository)) }

    init {
        // refresh index eagerly
        try {
            suggestionService.refreshIndexIfNeeded()
        } catch (_: Exception) {
            // ignore - indexing will be attempted again on demand
        }

        extend(
            CompletionType.BASIC,
            //only in dart files
            PlatformPatterns.psiElement().withLanguage(Language.findLanguageByID("Dart")!!),
            object : CompletionProvider<CompletionParameters>() {
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
                                    val element = insertionContext.file.findElementAt(startOffset) ?: return@withInsertHandler

                                    val newStatement = statementFixer.createStatement(element, item.lookupString)

                                    // Replace the typed prefix with the full statement
                                    document.replaceString(startOffset, tailOffset, newStatement)
                                    editor.caretModel.moveToOffset(startOffset + newStatement.length)
                                    importFixer.addTranslationImportIfMissing(project, element)
                                }
                            result.addElement(lookup)
                        }
                    } catch (e: Exception) {
                        // on any failure just don't add suggestions
                    }
                }
            }
        )
    }
}