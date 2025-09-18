package de.keeyzar.gpthelper.gpthelper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.common.error.GeneralErrorHandler
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessL10nClient
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.controller.MultiKeyTranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.*
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.client.GeminiBestGuessClient
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.parser.BestGuessOpenAIResponseParser
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service.OpenAIMultiKeyTranslationTaskSizeEstimator
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service.PsiElementIdReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions.AutoLocalizeOrchestrator
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.service.IdeaWaitingIndicatorService
import de.keeyzar.gpthelper.gpthelper.features.changetranslation.domain.controller.ChangeTranslationController
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.controller.FileTranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.factories.TranslationRequestFactory
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.FinishedFileTranslationHandler
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.GatherFileTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.PartialFileResponseHandler
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.factories.TranslationRequestFactoryImpl
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service.*
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.presentation.service.IdeaTargetLanguageProvider
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository.FlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.repository.FlutterFileRepository
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.repository.IdeaFlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.controller.MissingTranslationController
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.repository.ExistingTranslationRepository
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.service.MissingTranslationCollectionService
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.service.MissingTranslationInputService
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.infrastructure.repository.ExistingTranslationRepositoryIdea
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.infrastructure.service.MissingTranslationCollectionServiceIdea
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.infrastructure.service.MissingTranslationInputServiceIdea
import de.keeyzar.gpthelper.gpthelper.features.psiutils.*
import de.keeyzar.gpthelper.gpthelper.features.psiutils.arb.ArbPsiUtils
import de.keeyzar.gpthelper.gpthelper.features.psiutils.filter.DartStringLiteralFilter
import de.keeyzar.gpthelper.gpthelper.features.psiutils.filter.ImportStatementFilterDartString
import de.keeyzar.gpthelper.gpthelper.features.review.domain.config.ReviewConfig
import de.keeyzar.gpthelper.gpthelper.features.review.domain.repository.ReviewRepository
import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.AskUserForReviewService
import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.IdeaAskUserForReviewService
import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.OpenPageService
import de.keeyzar.gpthelper.gpthelper.features.review.domain.service.ReviewService
import de.keeyzar.gpthelper.gpthelper.features.review.infrastructure.mapper.ReviewSettingsMapper
import de.keeyzar.gpthelper.gpthelper.features.review.infrastructure.repository.IdeaReviewRepository
import de.keeyzar.gpthelper.gpthelper.features.review.infrastructure.service.IdeaOpenPageService
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.AppReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.SetupService
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.UserInstallDialogs
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.YamlModificationService
import de.keeyzar.gpthelper.gpthelper.features.setup.infrastructure.service.IdeaAppReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.setup.infrastructure.service.IdeaUserInstallDialogs
import de.keeyzar.gpthelper.gpthelper.features.setup.infrastructure.service.IdeaYamlModificationService
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.InstallFileProvider
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.PathProvider
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.ThreadingService
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.service.IdeaInstallFileProvider
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.service.IdeaPathProvider
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.service.L10NContentService
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.JsonUtils
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.ObjectMapperProvider
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.mapper.UserSettingsDTOMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.*
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.controller.TranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.mapper.TranslationRequestMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.ArbFilenameParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.UserTranslationInputParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationCredentialsServiceRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserTranslationInputRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.*
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client.*
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.LLMConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.TranslationRequestResponseMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.UserSettingsMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.parser.ARBFileContentParser
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.parser.GPTARBResponseParser
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.IdeaTranslationCredentialsServiceRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.LanguageFileFinder
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.PropertiesUserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.PropertiesUserTranslationInputRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.PsiTranslationFileRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.*
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.*
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.validation.FlutterIntlValidator
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.validation.TranslationClientSettingsValidator
import org.koin.core.module.Module
import org.koin.dsl.bind
import org.koin.dsl.module
import org.mapstruct.factory.Mappers

fun createAppModule(project: Project): Module {
    return module {
        single { project } bind Project::class
        single<ARBTemplateService> { ARBTemplateService() }
        single<GPTARBResponseParser> { GPTARBResponseParser(get()) }
        single<ObjectMapper> { ObjectMapperProvider().provideObjectMapper(null) }
        single<ARBFileContentParser> { ARBFileContentParser(get(), get()) }
        single<SingleTranslationRequestClient> { GPTARBRequester(get(), get(), get(), get()) }
        single<LLMConfigProvider> { LLMConfigProvider(get()) }
        single<ImmediateTranslationService> { ImmediateTranslationService(get(), get()) }
        single<UserSettingsRepository> { PropertiesUserSettingsRepository(get(), get()) }
        single<JsonUtils> { JsonUtils(get()) }
        single<JsonChunkMerger> { JsonChunkMerger(get()) }
        single<JsonFileChunker> { JsonFileChunker(get()) }
        single<TranslationProgressBus> { IdeaTranslationProgressBus(get()) }
        single<IdeaTerminalConsoleService> { IdeaTerminalConsoleService() }
        single<FlutterIntlSettingsRepository> {
            IdeaFlutterIntlSettingsRepository(
                get(),
                ObjectMapperProvider().provideObjectMapper(YAMLFactory()),
                get(),
                get()
            )
        }
        single<FlutterIntlValidator> { FlutterIntlValidator() }
        single<TranslationFileRepository> { PsiTranslationFileRepository(get(), get(), get()) }
        single<ContentModificationService> { ArbContentModificationService(get()) }
        single<TranslationRequestMapper> { Mappers.getMapper(TranslationRequestMapper::class.java) }
        single<DispatcherConfiguration> { DispatcherConfiguration(get(), get()) }
        single<DDDTranslationRequestClient> {
            GPTTranslationRequestClient(
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        single<ArbFileModificationService> { ArbFileModificationService(get(), get(), get()) }
        single<VerifyTranslationSettingsService> {
            ArbVerifyTranslationSettingsService(
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        single<TranslationClientSettingsValidator> { TranslationClientSettingsValidator(get()) }
        single<UserTranslationInputRepository> { PropertiesUserTranslationInputRepository(get(), get()) }
        single<UserTranslationInputParser> { UserTranslationInputParser(get()) }
        single<ArbFilenameParser> { ArbFilenameParser() }
        single<CurrentFileModificationService> {
            FlutterArbCurrentFileModificationService(
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        single<ExternalTranslationProcessService> { FlutterGenCommandProcessService(get(), get()) }
        single<TranslationErrorProcessHandler> { IdeaTranslationErrorProcessHandlerImpl() }
        single<FlutterPsiService> { FlutterPsiService() }
        single<TranslationProcessController> {
            TranslationProcessController(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        single<TranslationPreprocessor> { TranslationPreprocessor(get(), get(), get()) }
        single<LastStatementProviderForFlutterArbTranslation> { LastStatementProviderForFlutterArbTranslation() }
        single<LanguageFileFinder> { LanguageFileFinder(get()) }
        single<TranslationRequestResponseMapper> { TranslationRequestResponseMapper(get()) }
        single<ImportFixer> { ImportFixer(get()) }
        single<L10NContentService> { L10NContentService(project) }
        single<StatementFixer> { StatementFixer(get(), get(), get(), get()) }
        single<TranslationTaskBackgroundProgress> { TranslationTaskBackgroundProgress(get()) }
        single<UserSettingsMapper> { Mappers.getMapper(UserSettingsMapper::class.java) }
        single<FlutterFileRepository> { FlutterFileRepository() }
        single<FormatTranslationFileContentService> { ArbFormatTranslationFileContentService(get()) }
        single<TaskAmountCalculator> { TranslateKeyTaskAmountCalculator() }
        single<TranslationTriggeredHooks> { TranslateKeyTranslationTriggeredHooks(get(), get()) }
        single<FlutterGenCommandProcessService> { FlutterGenCommandProcessService(get(), get()) }
        single<OngoingTranslationHandler> { OngoingTranslationHandler(get(), get(), get()) }
        single<FlutterArbCurrentFileModificationService> {
            FlutterArbCurrentFileModificationService(
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        single<ContextProvider> { ContextProvider() }
        single<GatherFileTranslationContext> { IdeaGatherFileTranslationContext(get(), get(), get()) }
        single<TargetLanguageProvider> { TargetLanguageProvider(get()) }
        single<PartialFileResponseHandler> { ArbFlutterPartialFileTranslationResponseHandler(get(), get()) }
        single<TranslationRequestFactory> { TranslationRequestFactoryImpl(get()) }
        single<FileTranslationProcessController> {
            FileTranslationProcessController(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        single<TargetLanguageProvider> { IdeaTargetLanguageProvider(get()) }
        single<ArbFileContentModificationService> { ArbFileContentModificationService(get()) }
        single<FinishedFileTranslationHandler> { FlutterArbTranslateFileFinished(get(), get(), get()) }
        single<UserSettingsMapper> { Mappers.getMapper(UserSettingsMapper::class.java) }
        single<TranslationCredentialsServiceRepository> { IdeaTranslationCredentialsServiceRepository() }
        single<AutoLocalizeOrchestrator> { AutoLocalizeOrchestrator(get(), get()) }
        single<UserSettingsDTOMapper> { Mappers.getMapper(UserSettingsDTOMapper::class.java) }
        single<ClientConnectionTester> { OpenAIClientConnectionTester(get()) }
        single<DartAdditiveExpressionExtractor> { DartAdditiveExpressionExtractor() }
        single<DartStringLiteralHelper> { DartStringLiteralHelper(get(), getAll()) }
        single { ImportStatementFilterDartString() } bind DartStringLiteralFilter::class
        single<LiteralInContextFinder> { LiteralInContextFinder() }
        single<PsiElementIdGenerator> { PsiElementIdGenerator() }
        single<GatherBestGuessContext> { IdeaGatherBestGuessContext(get(), get(), get()) }
        single<BestGuessOpenAIResponseParser> { BestGuessOpenAIResponseParser(get()) }
        single<BestGuessL10nClient> { GeminiBestGuessClient(get(), get(), get(), get()) }
        single<PsiElementIdReferenceProvider> { PsiElementIdReferenceProvider() }
        single<GuessAdaptionService> { IdeaBestGuessAdaptionService(get(), get(), get(), get()) }
        single<ArbFilesService> { ArbFilesService(get(), get(), get(), get()) }
        single<MultiKeyTranslationProcessController> {
            MultiKeyTranslationProcessController(
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        single<MultiKeyTranslationTaskSizeEstimator> { OpenAIMultiKeyTranslationTaskSizeEstimator() }
        single<WaitingIndicatorService> { IdeaWaitingIndicatorService(get()) }
        single<DartConstModifierFinder> { DartConstModifierFinder() }
        single<ReviewSettingsMapper> { Mappers.getMapper(ReviewSettingsMapper::class.java) }
        single<ReviewRepository> { IdeaReviewRepository(get(), get()) }
        single<OpenPageService> { IdeaOpenPageService() }
        single<ReviewService> { ReviewService(get(), get(), get(), get()) }
        single<AskUserForReviewService> { IdeaAskUserForReviewService() }
        single<ReviewConfig> { ReviewConfig() }
        single<GPTModelProvider> { GptModelProviderImpl(get()) }
        single<ArbPsiUtils> { ArbPsiUtils() }
        single<ChangeTranslationController> { ChangeTranslationController() }
        single<ThreadingService<MissingTranslationContext<TranslationContext>>> { ThreadingService() }
        single<MissingTranslationController<PsiElement>> {
            MissingTranslationController(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get()
            )
        }
        single<GatherTranslationContextService> { FlutterArbTranslationContextService(get(), get(), get(), get(), get(), get())}
        single<MissingTranslationCollectionService<PsiElement>> { MissingTranslationCollectionServiceIdea(get()) }
        single<ExistingTranslationRepository<PsiElement>> { ExistingTranslationRepositoryIdea(get(), get()) }
        single<MissingTranslationInputService> { MissingTranslationInputServiceIdea() }
        single<ArbPsiUtils> { ArbPsiUtils() }
        single<TranslationFileRepository> { PsiTranslationFileRepository(get(), get(), get()) }
        single<ThreadingService<TranslationContext>> { ThreadingService() }
        single<GeneralErrorHandler> { GeneralErrorHandler() }
        single<ExistingKeyFinder> { ExistingKeyFinder(get(), get()) }
        single<GatherUserInputService> { FlutterArbUserInputService(get()) }
        single<SetupService> { SetupService(get(), get(), get(), get(), get()) }
        single<PathProvider> { IdeaPathProvider(project) }
        single<UserInstallDialogs> { IdeaUserInstallDialogs(project) }
        single<InstallFileProvider> { IdeaInstallFileProvider(project) }
        single<AppReferenceProvider> { IdeaAppReferenceProvider(project, get(), get()) }
        single<YamlModificationService> { IdeaYamlModificationService(project) }
    }
}
