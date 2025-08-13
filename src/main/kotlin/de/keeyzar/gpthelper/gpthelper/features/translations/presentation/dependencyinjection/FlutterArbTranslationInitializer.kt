package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection

import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessL10nClient
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.controller.MultiKeyTranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.GatherBestGuessContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.GuessAdaptionService
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.WaitingIndicatorService
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service.PsiElementIdReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.psiutils.arb.ArbPsiUtils
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.controller.FileTranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.controller.MissingTranslationController
import de.keeyzar.gpthelper.gpthelper.features.psiutils.DartStringLiteralHelper
import de.keeyzar.gpthelper.gpthelper.features.psiutils.LiteralInContextFinder
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.controller.TranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.UserTranslationInputParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.OngoingTranslationHandler
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.TranslationPreprocessor
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.TranslationTriggeredHooks
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.CurrentProjectProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.ContextProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.FlutterPsiService
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.LastStatementProviderForFlutterArbTranslation
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.TranslationTaskBackgroundProgress
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * provides all the relevant dependencies for the translation feature, there might be a cleaner way to do that... but okay for now
 */
class FlutterArbTranslationInitializer : KoinComponent {
    val flutterPsiService: FlutterPsiService by inject()
    val translationProcessController: TranslationProcessController by inject()
    val missingTranslationController: MissingTranslationController<PsiElement> by inject()
    val translationTaskBackgroundProgress: TranslationTaskBackgroundProgress by inject()
    val lastStatementProviderForFlutterArbTranslation: LastStatementProviderForFlutterArbTranslation by inject()
    val fileTranslationProcessController: FileTranslationProcessController by inject()
    val contextProvider: ContextProvider by inject()
    val literalInContextFinder: LiteralInContextFinder by inject()
    val multiKeyTranslationProcessController: MultiKeyTranslationProcessController by inject()
    val currentProjectProvider: CurrentProjectProvider by inject()
    val psiElementIdReferenceProvider: PsiElementIdReferenceProvider by inject()
    val arbPsiUtils: ArbPsiUtils by inject()
    val translationPreprocessor: TranslationPreprocessor by inject()
    val userTranslationInputParser: UserTranslationInputParser by inject()
    val ongoingTranslationHandler: OngoingTranslationHandler by inject()
    val translationTriggeredHooks: TranslationTriggeredHooks by inject()
    val gatherBestGuessContext: GatherBestGuessContext by inject()
    val waitingIndicatorService: WaitingIndicatorService by inject()
    val bestGuessL10nClient: BestGuessL10nClient by inject()
    val guessAdaptionService: GuessAdaptionService by inject()
    val dartStringLiteralHelper: DartStringLiteralHelper by inject()
}
