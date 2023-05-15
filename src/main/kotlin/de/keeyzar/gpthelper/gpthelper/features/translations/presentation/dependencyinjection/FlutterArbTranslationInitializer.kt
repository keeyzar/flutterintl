package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.controller.BestGuessProcessController
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service.PsiElementIdReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.controller.FileTranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.psiutils.LiteralInContextFinder
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.controller.TranslationProcessController
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
    val translationTaskBackgroundProgress: TranslationTaskBackgroundProgress by inject()
    val lastStatementProviderForFlutterArbTranslation: LastStatementProviderForFlutterArbTranslation by inject()
    val fileTranslationProcessController: FileTranslationProcessController by inject()
    val contextProvider: ContextProvider by inject()
    val literalInContextFinder: LiteralInContextFinder by inject()
    val bestGuessProcessController: BestGuessProcessController by inject()
    val currentProjectProvider: CurrentProjectProvider by inject()
    val psiElementIdReferenceProvider: PsiElementIdReferenceProvider by inject()
}
