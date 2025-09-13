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
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.VerifyTranslationSettingsService
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class AutoLocalizeOrchestrator(
    private val generalErrorHandler: GeneralErrorHandler,
    private val verifySettings: VerifyTranslationSettingsService,
) {
    private val initializer = FlutterArbTranslationInitializer()

    fun orchestrate(project: Project, directory: VirtualFile, title: String) {
        val dartFiles = findDartFiles(project, directory)
        val stringLiteralHelper = initializer.dartStringLiteralHelper
        val allLiteralsWithSelection: Map<PsiElement, Boolean> =
            dartFiles.flatMap { stringLiteralHelper.findStringPsiElements(it).entries }
                .associate { it.key to it.value }

        if (allLiteralsWithSelection.isEmpty()) {
            return
        }

        val existingKeys = initializer.existingKeyFinder.findExistingKeys(allLiteralsWithSelection.keys.toList())

        var userChoiceForExistingKeys: Map<PsiElement, Boolean> = emptyMap()
        if (existingKeys.isNotEmpty()) {
            ApplicationManager.getApplication().invokeAndWait {
                val dialog = ExistingKeysDialog(project, existingKeys)
                if (dialog.showAndGet()) {
                    userChoiceForExistingKeys = dialog.getSelectedElements()
                } else {
                    // User cancelled, so we assume they don't want to replace anything
                    userChoiceForExistingKeys = existingKeys.keys.associateWith { false }
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

                elementsByFile.forEach { (file, elements) ->
                    importFixer.addTranslationImportIfMissing(project, file)
                    elements.forEach { element ->
                        val key = existingKeys[element]
                        if (key != null) {
                            statementFixer.fixStatement(project, element, key)
                        }
                    }
                }
            }
        }

        val remainingLiterals = allLiteralsWithSelection.filterKeys { it !in elementsToReplace }

        if (remainingLiterals.isEmpty()) {
            //everything is replaced, so we are done
            return
        }

        var selectedLiterals: List<PsiElement>? = null
        ApplicationManager.getApplication().invokeAndWait {
            val dialog = CollectedStringsDialog(project, remainingLiterals)
            if (dialog.showAndGet()) {
                selectedLiterals = dialog.getSelectedLiterals()
            }
        }

        val finalLiterals = selectedLiterals
        if (finalLiterals.isNullOrEmpty()) {
            return
        }

        val initializer = FlutterArbTranslationInitializer()
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
