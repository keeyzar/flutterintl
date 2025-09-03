package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runReadAction
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
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.FlutterPsiService
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class AutoLocalizeOrchestrator(
    private val generalErrorHandler: GeneralErrorHandler
) {
    private val initializer = FlutterArbTranslationInitializer()

    fun orchestrate(project: Project, directory: VirtualFile, title: String) {
        val dartFiles = findDartFiles(project, directory)
        val stringLiteralHelper = initializer.dartStringLiteralHelper
        val allLiteralsWithSelection =
            dartFiles.flatMap { stringLiteralHelper.findStringPsiElements(it).entries }
                .associate { it.key to it.value }

        if (allLiteralsWithSelection.isEmpty()) {
            return
        }

        var selectedLiterals: List<PsiElement>? = null
        ApplicationManager.getApplication().invokeAndWait {
            val dialog = CollectedStringsDialog(project, allLiteralsWithSelection)
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
