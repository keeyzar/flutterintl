package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.jetbrains.lang.dart.DartFileType
import de.keeyzar.gpthelper.gpthelper.common.error.GeneralErrorHandler
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessRequest
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.client.GeminiBestGuessClient
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.model.AutoLocalizeContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets.CollectedStringsDialog
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets.ExistingKeysDialog
import de.keeyzar.gpthelper.gpthelper.features.psiutils.SmartPsiElementWrapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.VerifyTranslationSettingsService
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class AutoLocalizeOrchestrator(
    private val generalErrorHandler: GeneralErrorHandler,
    private val verifySettings: VerifyTranslationSettingsService,
) {

    fun orchestrate(project: Project, directory: VirtualFile, title: String) {
        val dartFiles = findDartFiles(project, directory)
        if (dartFiles.isEmpty()) {
            return
        }

        val initializer = FlutterArbTranslationInitializer.create(project)
        val stringLiteralHelper = initializer.dartStringLiteralHelper
        val allLiteralsWithSelection: Map<SmartPsiElementWrapper<PsiElement>, Boolean> =
            dartFiles.flatMap { stringLiteralHelper.findStringPsiElements(it).entries }
                .associate { it.key to it.value }

        if (allLiteralsWithSelection.isEmpty()) {
            return
        }

        // Resolve wrappers to get actual PSI elements for existing key finder
        val resolvedElements = SmartPsiElementWrapper.unwrapList(allLiteralsWithSelection.keys.toList())
        val existingKeys = initializer.existingKeyFinder.findExistingKeys(resolvedElements)

        var userChoiceForExistingKeys: Map<PsiElement, Boolean> = emptyMap()
        if (existingKeys.isNotEmpty()) {
            ApplicationManager.getApplication().invokeAndWait {
                val dialog = ExistingKeysDialog(project, existingKeys)
                if (dialog.showAndGet()) {
                    userChoiceForExistingKeys = dialog.getSelectedElements()
                } else {
                    // User cancelled, so we assume they don't want to replace anything
                    // Resolve existing keys to PsiElements
                    val resolvedKeys = SmartPsiElementWrapper.unwrapMap(existingKeys)
                    userChoiceForExistingKeys = resolvedKeys.keys.associateWith { false }
                }
            }
        }

        val elementsToReplace = userChoiceForExistingKeys.filterValues { it }.keys

        if (elementsToReplace.isNotEmpty()) {
            //we need a write action, because we want to modify the psi tree
            WriteCommandAction.runWriteCommandAction(project) {
                val statementFixer = initializer.statementFixer
                val importFixer = initializer.importFixer
                val elementsByFile = elementsToReplace.groupBy { it.containingFile }

                // Resolve existingKeys map to get the actual key strings
                val resolvedExistingKeys = SmartPsiElementWrapper.unwrapMap(existingKeys)

                elementsByFile.forEach { (file, elements) ->
                    importFixer.addTranslationImportIfMissing(project, file)
                    elements.forEach { element ->
                        val key = resolvedExistingKeys[element]
                        if (key != null) {
                            statementFixer.fixStatement(project, element, key)
                        }
                    }
                }
            }
        }

        val remainingLiterals = allLiteralsWithSelection.filterKeys { wrapper ->
            val element = runReadAction { wrapper.element }
            element !in elementsToReplace
        }

        if (remainingLiterals.isEmpty()) {
            // No remaining literals to process (either all replaced or nothing to do)
            return
        }

        // NEW: AI-based pre-filtering
        val preFilteredLiterals = performAiPreFiltering(project, initializer, remainingLiterals)

        var selectedLiterals: List<PsiElement>? = null
        ApplicationManager.getApplication().invokeAndWait {
            val dialog = CollectedStringsDialog(project, preFilteredLiterals)
            if (dialog.showAndGet()) {
                selectedLiterals = dialog.getSelectedLiterals()
            }
        }

        val finalLiterals = selectedLiterals
        if (finalLiterals.isNullOrEmpty()) {
            return
        }

        val uuid = UUID.randomUUID()
        val translationContext = TranslationContext(uuid.toString(), title, 0, null, 0)

        val verified = verifySettings.verifySettingsAndInformUserIfInvalid()
        if (!verified) {
            return
        }

        initializer.translationTaskBackgroundProgress.triggerInBlockingContext(project,
            {
                try {
                    val baseFile = runReadAction {
                        finalLiterals.first().containingFile
                    }
                    initializer.contextProvider.putAutoLocalizeContext(
                        uuid,
                        AutoLocalizeContext(project, baseFile)
                    )

                    val fileBestGuessContext = runReadAction {
                        initializer.gatherBestGuessContext.fromPsiElements(uuid, finalLiterals)
                    } ?: return@triggerInBlockingContext

                    initializer.waitingIndicatorService.startWaiting(
                        uuid,
                        "Requesting AI Suggestions",
                        "Getting translation key guesses. This might take a while, please be patient"
                    )
                    val guessCounter = AtomicInteger(0)
                    val totalGuesses = (fileBestGuessContext.literals.size / GeminiBestGuessClient.PARALLELISM_THRESHOLD).coerceAtLeast(1)
                    val simpleGuess =
                        initializer.bestGuessL10nClient.simpleGuess(BestGuessRequest(fileBestGuessContext)) {
                            val currentGuess = guessCounter.incrementAndGet()
                            initializer.waitingIndicatorService.updateProgress(
                                uuid,
                                "Creating best guesses: $currentGuess/$totalGuesses"
                            )
                        }
                    initializer.waitingIndicatorService.stopWaiting()

                    if (simpleGuess.responseEntries.isEmpty()) {
                        return@triggerInBlockingContext
                    }

                    val multiKeyTranslationContext = initializer.guessAdaptionService.adaptBestGuess(uuid, simpleGuess)
                        ?: return@triggerInBlockingContext

                    val multiKeyTranslationContextWithUuid = multiKeyTranslationContext.copy(uuid = uuid.toString())

                    translationContext.taskAmount = multiKeyTranslationContextWithUuid.targetLanguages.size * simpleGuess.responseEntries.size
                    translationContext.progressText = "Auto Localize"

                    initializer.multiKeyTranslationProcessController.startTranslationProcess(multiKeyTranslationContextWithUuid)
                } catch (throwable: Throwable) {
                    generalErrorHandler.handleError(project, throwable)
                } finally {
                    translationContext.finished = true
                }
            },
            translationContext = translationContext,
            finishedCallback = {
                initializer.psiElementIdReferenceProvider.deleteAllElements()
                initializer.contextProvider.removeAutoLocalizeContext(uuid)
            }
        )
    }

    private fun performAiPreFiltering(
        project: Project,
        initializer: FlutterArbTranslationInitializer,
        remainingLiterals: Map<SmartPsiElementWrapper<PsiElement>, Boolean>
    ): Map<SmartPsiElementWrapper<PsiElement>, Boolean> {
        if (remainingLiterals.isEmpty()) {
            return remainingLiterals
        }

        try {
            // Separate already-filtered (false) from candidates (true)
            val alreadyFiltered = remainingLiterals.filterValues { !it }
            val candidatesForAi = remainingLiterals.filterValues { it }

            // If no candidates need AI filtering, return as-is
            if (candidatesForAi.isEmpty()) {
                return remainingLiterals
            }

            // Build pre-filter request only for candidates
            val preFilterRequest = runReadAction {
                initializer.preFilterRequestBuilder.buildRequest(candidatesForAi)
            }

            val totalBatches = estimateBatches(preFilterRequest.literals.size)

            // Perform AI pre-filtering with blocking progress dialog
            val preFilterResponse = com.intellij.openapi.progress.ProgressManager.getInstance().run(
                object : com.intellij.openapi.progress.Task.WithResult<de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterResponse, Exception>(
                    project,
                    if (totalBatches > 1) {
                        "Pre-filtering ${preFilterRequest.literals.size} strings in $totalBatches batches..."
                    } else {
                        "Pre-filtering ${preFilterRequest.literals.size} strings, one moment please..."
                    },
                    true
                ) {
                    override fun compute(indicator: com.intellij.openapi.progress.ProgressIndicator): de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterResponse {
                        indicator.isIndeterminate = false

                        return kotlinx.coroutines.runBlocking {
                            initializer.preFilterClient.preFilter(preFilterRequest) { current, total ->
                                indicator.fraction = current.toDouble() / total.toDouble()
                                indicator.text = "Pre-filtering batch $current of $total..."
                            }
                        }
                    }
                }
            )

            // Map results back to SmartPsiElementWrapper
            val resultMap = mutableMapOf<String, Boolean>()
            preFilterResponse.results.forEach { result ->
                resultMap[result.id] = result.shouldTranslate
            }

            // Create a map from PsiElement to wrapper for reverse lookup
            val elementToWrapper = runReadAction {
                candidatesForAi.mapNotNull { (wrapper, _) ->
                    wrapper.element?.let { it to wrapper }
                }.toMap()
            }

            // Update selection based on AI results for candidates
            val aiFilteredLiterals = mutableMapOf<SmartPsiElementWrapper<PsiElement>, Boolean>()
            preFilterRequest.literals.forEach { literal ->
                val shouldTranslate = resultMap[literal.id] ?: false
                val wrapper = elementToWrapper[literal.psiElement]
                if (wrapper != null) {
                    aiFilteredLiterals[wrapper] = shouldTranslate
                }
            }

            // Merge with already-filtered strings (keep them as false)
            return alreadyFiltered + aiFilteredLiterals
        } catch (e: Exception) {
            // On error, fall back to original selection
            generalErrorHandler.handleError(project, e)
            return remainingLiterals
        }
    }

    private fun estimateBatches(literalCount: Int): Int {
        val estimatedTokensPerLiteral = 50
        val maxTokensPerBatch = 5000
        val totalEstimatedTokens = literalCount * estimatedTokensPerLiteral
        return (totalEstimatedTokens / maxTokensPerBatch).coerceAtLeast(1)
    }
}


fun findDartFiles(project: Project, directory: VirtualFile): List<PsiFile> {
    return runReadAction {
        val directoryScope = GlobalSearchScopes.directoryScope(project, directory, true)
        val psiManager = PsiManager.getInstance(project)
        FileTypeIndex.getFiles(DartFileType.INSTANCE, directoryScope).mapNotNull {
            psiManager.findFile(it)
        }
    }
}
