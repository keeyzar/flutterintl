package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessL10nClient
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.controller.MultiKeyTranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.ExistingKeyFinder
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.GatherBestGuessContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.GuessAdaptionService
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.WaitingIndicatorService
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service.PsiElementIdReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions.AutoLocalizeOrchestrator
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.controller.FileTranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.controller.MissingTranslationController
import de.keeyzar.gpthelper.gpthelper.features.psiutils.DartStringLiteralHelper
import de.keeyzar.gpthelper.gpthelper.features.psiutils.LiteralInContextFinder
import de.keeyzar.gpthelper.gpthelper.features.psiutils.arb.ArbPsiUtils
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.SetupService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.controller.TranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.ContextProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.LastStatementProviderForFlutterArbTranslation
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.FlutterPsiService
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.ImportFixer
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.StatementFixer
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.TranslationTaskBackgroundProgress
import de.keeyzar.gpthelper.gpthelper.project.ProjectKoinService

/**
 * provides all the relevant dependencies for the translation feature, there might be a cleaner way to do that... but okay for now
 */
class FlutterArbTranslationInitializer(
    val flutterPsiService: FlutterPsiService,
    val translationProcessController: TranslationProcessController,
    val missingTranslationController: MissingTranslationController<PsiElement>,
    val translationTaskBackgroundProgress: TranslationTaskBackgroundProgress,
    val lastStatementProviderForFlutterArbTranslation: LastStatementProviderForFlutterArbTranslation,
    val fileTranslationProcessController: FileTranslationProcessController,
    val contextProvider: ContextProvider,
    val literalInContextFinder: LiteralInContextFinder,
    val multiKeyTranslationProcessController: MultiKeyTranslationProcessController,
    val psiElementIdReferenceProvider: PsiElementIdReferenceProvider,
    val arbPsiUtils: ArbPsiUtils,
    val gatherBestGuessContext: GatherBestGuessContext,
    val waitingIndicatorService: WaitingIndicatorService,
    val bestGuessL10nClient: BestGuessL10nClient,
    val guessAdaptionService: GuessAdaptionService,
    val dartStringLiteralHelper: DartStringLiteralHelper,
    val orchestrator: AutoLocalizeOrchestrator,
    val existingKeyFinder: ExistingKeyFinder,
    val statementFixer: StatementFixer,
    val importFixer: ImportFixer,
    val setupService: SetupService,
) {
    companion object {
        fun create(project: Project): FlutterArbTranslationInitializer {
            val koin = ProjectKoinService.getInstance(project).getKoin()
            return FlutterArbTranslationInitializer(
                flutterPsiService = koin.get(),
                translationProcessController = koin.get(),
                missingTranslationController = koin.get(),
                translationTaskBackgroundProgress = koin.get(),
                lastStatementProviderForFlutterArbTranslation = koin.get(),
                fileTranslationProcessController = koin.get(),
                contextProvider = koin.get(),
                literalInContextFinder = koin.get(),
                multiKeyTranslationProcessController = koin.get(),
                psiElementIdReferenceProvider = koin.get(),
                arbPsiUtils = koin.get(),
                gatherBestGuessContext = koin.get(),
                waitingIndicatorService = koin.get(),
                bestGuessL10nClient = koin.get(),
                guessAdaptionService = koin.get(),
                dartStringLiteralHelper = koin.get(),
                orchestrator = koin.get(),
                existingKeyFinder = koin.get(),
                statementFixer = koin.get(),
                importFixer = koin.get(),
                setupService = koin.get(),
            )
        }
    }
}
