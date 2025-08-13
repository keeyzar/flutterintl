package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScopes
import com.jetbrains.lang.dart.DartFileType
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessRequest
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.model.AutoLocalizeContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets.CollectedStringsDialog
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.util.*

class AutoLocalizeDirectory : DumbAwareAction() {

    override fun update(e: AnActionEvent) {
        val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE)
//        e.presentation.isEnabledAndVisible = virtualFile != null && virtualFile.isDirectory
    }

    override fun actionPerformed(event: AnActionEvent) {
        val initializer = FlutterArbTranslationInitializer()
        val uuid = UUID.randomUUID()
        val translationContext = TranslationContext(uuid.toString(), "Auto Localizing Directory", 0, null, 0)
        val project = event.project!!
        val directory = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return

        initializer.translationTaskBackgroundProgress.triggerInBlockingContext(project,
            {
                try {
                    val dartFiles = findDartFiles(project, directory)
                    if (dartFiles.isEmpty()) {
                        initializer.waitingIndicatorService.stopWaiting()
                        return@triggerInBlockingContext
                    }

                    initializer.contextProvider.putAutoLocalizeContext(
                        uuid,
                        AutoLocalizeContext(project, dartFiles.first())
                    )
                    initializer.waitingIndicatorService.startWaiting(
                        uuid,
                        "Scanning Directory",
                        "Finding all localizable strings..."
                    )

                    val stringLiteralHelper = initializer.dartStringLiteralHelper
                    val allLiteralsWithSelection =
                        dartFiles.flatMap { stringLiteralHelper.findStringPsiElements(it).entries }
                            .associate { it.key to it.value }

                    initializer.waitingIndicatorService.stopWaiting()

                    if (allLiteralsWithSelection.isEmpty()) {
                        return@triggerInBlockingContext
                    }

                    var selectedLiterals: List<PsiElement>? = null
                    ApplicationManager.getApplication().invokeAndWait {
                        val dialog = CollectedStringsDialog(project, allLiteralsWithSelection)
                        if (dialog.showAndGet()) {
                            selectedLiterals = dialog.getSelectedLiterals()
                        }
                    }

                    val finalLiterals = selectedLiterals
                    if (finalLiterals == null || finalLiterals.isEmpty()) {
                        return@triggerInBlockingContext
                    }

                    // We use the first file for context, but the logic could be more sophisticated
                    initializer.contextProvider.putAutoLocalizeContext(
                        uuid,
                        AutoLocalizeContext(project, finalLiterals.first().containingFile)
                    )

                    val fileBestGuessContext = initializer.gatherBestGuessContext.fromPsiElements(uuid, finalLiterals)
                        ?: return@triggerInBlockingContext

                    initializer.waitingIndicatorService.startWaiting(
                        uuid,
                        "Requesting AI Suggestions",
                        "Getting translation key guesses for directory. This might take a while, please be patient"
                    )
                    val simpleGuess =
                        initializer.bestGuessL10nClient.simpleGuess(BestGuessRequest(fileBestGuessContext))
                    initializer.waitingIndicatorService.stopWaiting()

                    if (simpleGuess.responseEntries.isEmpty()) {
                        return@triggerInBlockingContext
                    }

                    val multiKeyTranslationContext = initializer.guessAdaptionService.adaptBestGuess(uuid, simpleGuess)
                        ?: return@triggerInBlockingContext

                    initializer.multiKeyTranslationProcessController.startTranslationProcess(multiKeyTranslationContext)
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

    private fun findDartFiles(project: Project, directory: VirtualFile): List<PsiFile> {
        return ApplicationManager.getApplication().runReadAction<List<PsiFile>> {
            val directoryScope = GlobalSearchScopes.directoryScope(project, directory, true)
            val psiManager = PsiManager.getInstance(project)
            FileTypeIndex.getFiles(DartFileType.INSTANCE, directoryScope).mapNotNull {
                psiManager.findFile(it)
            }
        }
    }
}
