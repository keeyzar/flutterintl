package de.keeyzar.gpthelper.gpthelper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import de.keeyzar.gpthelper.gpthelper.features.ddd.domain.repository.DDDSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.ddd.infrastructure.mapper.DirectoryStructureMapper
import de.keeyzar.gpthelper.gpthelper.features.ddd.infrastructure.repository.PreferencesDDDSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.ddd.presentation.service.CreateDirectoryTreeService
import de.keeyzar.gpthelper.gpthelper.features.ddd.presentation.service.SaveDirectoryTreeService
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.TranslationClient
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.controller.FileTranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.factories.TranslationRequestFactory
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.FinishedFileTranslationHandler
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.GatherFileTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.service.PartialFileResponseHandler
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.client.GPTTranslationClient
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.factories.TranslationRequestFactoryImpl
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service.*
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.presentation.service.IdeaTargetLanguageProvider
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository.FlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.repository.FlutterFileRepository
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.repository.IdeaFlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.JsonUtils
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.ObjectMapperProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.DDDTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.SingleTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.TaskAmountCalculator
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.controller.TranslationProcessController
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.mapper.TranslationRequestMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.ArbFilenameParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.UserTranslationInputParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationFileRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserTranslationInputRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.*
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client.GPTARBRequester
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client.GPTTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client.TranslateKeyTaskAmountCalculator
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.configuration.OpenAIConfigProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.TranslationRequestResponseMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.mapper.UserSettingsMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.parser.ARBFileContentParser
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.parser.GPTARBResponseParser
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.*
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.*
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.*
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.validation.FlutterIntlValidator
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.validation.TranslationClientSettingsValidator
import org.koin.dsl.module
import org.mapstruct.factory.Mappers

/**
 * this one must be refactored...
 */
class DIConfig {
    companion object {
        val appModule = module {
            single<ARBTemplateService> { ARBTemplateService() }
            single<GPTARBResponseParser> { GPTARBResponseParser(get()) }
            single<ObjectMapper> { ObjectMapperProvider().provideObjectMapper(null) }
            single<ARBFileContentParser> { ARBFileContentParser(get(), get()) }
            single<SingleTranslationRequestClient> { GPTARBRequester(get(), get(), get()) }
            single<OpenAIConfigProvider> { OpenAIConfigProvider(get()) }
            single<ImmediateTranslationService> { ImmediateTranslationService(get(), get()) }
            single<UserSettingsRepository> { PropertiesUserSettingsRepository(get(), get(), get()) }
            single<DDDSettingsRepository> { PreferencesDDDSettingsRepository(get(), get()) }
            single<DirectoryStructureMapper> { Mappers.getMapper(DirectoryStructureMapper::class.java) }
            single<CreateDirectoryTreeService> { CreateDirectoryTreeService(get()) }
            single<SaveDirectoryTreeService> { SaveDirectoryTreeService(get()) }
            single<JsonUtils> { JsonUtils(get()) }
            single<JsonChunkMerger> { JsonChunkMerger(get()) }
            single<JsonFileChunker> { JsonFileChunker(get()) }
            val ideaTranslationPercentageBus = IdeaTranslationProgressBus()
            single<TranslationProgressBus> { ideaTranslationPercentageBus }
            single<IdeaTranslationProgressBus> { ideaTranslationPercentageBus }
            single<WholeFileTranslationService> { WholeFileTranslationService(get()) }
            single<IdeaTerminalConsoleService> { IdeaTerminalConsoleService() }
            single<CurrentProjectProvider> { CurrentProjectProvider() }
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
            single<DDDTranslationService> { DDDTranslationService(get(), get()) }
            single<DDDTranslationRequestClient> { GPTTranslationRequestClient(get(), get()) }
            single<ArbFileModificationService> { ArbFileModificationService(get(), get(), get()) }
            single<VerifyTranslationSettingsService> { ArbVerifyTranslationSettingsService(get(), get(), get(), get(), get()) }
            single<TranslationClientSettingsValidator> { TranslationClientSettingsValidator() }
            single<UserTranslationInputRepository> { PropertiesUserTranslationInputRepository(get()) }
            single<GatherTranslationContextService> { FlutterArbTranslationContextService(get(), get(), get(), get(), get(), get()) }
            single<GatherUserInputService> { FlutterArbUserInputService() }
            single<UserTranslationInputParser> { UserTranslationInputParser(get()) }
            single<ArbFilenameParser> { ArbFilenameParser() }
            single<CurrentFileModificationService> { FlutterArbCurrentFileModificationService(get(), get(), get(), get()) }
            single<ExternalTranslationProcessService> { FlutterGenCommandProcessService(get(), get()) }
            single<TranslationErrorProcessHandler> { IdeaTranslationErrorProcessHandlerImpl() }
            single<FlutterPsiService> { FlutterPsiService() }
            single<TranslationProcessController> { TranslationProcessController(get(), get(), get(), get(), get(), get(), get()) }
            single<TranslationPreprocessor> { TranslationPreprocessor(get(), get(), get()) }
            single<LastStatementProviderForFlutterArbTranslation> { LastStatementProviderForFlutterArbTranslation() }
            single<LanguageFileFinder> { LanguageFileFinder(get()) }
            single<TranslationRequestResponseMapper> { TranslationRequestResponseMapper(get()) }
            single<ImportFixer> { ImportFixer(get()) }
            single<StatementFixer> { StatementFixer(get()) }
            single<TranslationTaskBackgroundProgress> { TranslationTaskBackgroundProgress() }
            single<UserSettingsMapper> { Mappers.getMapper(UserSettingsMapper::class.java) }
            single<FlutterFileRepository> { FlutterFileRepository() }
            single<FormatTranslationFileContentService> { ArbFormatTranslationFileContentService(get()) }
            single<TaskAmountCalculator> { TranslateKeyTaskAmountCalculator() }
            single<PostTranslationTriggerService> { TranslateKeyPostTranslationTriggerService(get(), get()) }
            single<FlutterGenCommandProcessService> { FlutterGenCommandProcessService(get(), get()) }
            single<OngoingTranslationHandler> { OngoingTranslationHandler(get(), get(), get()) }
            single<FlutterArbCurrentFileModificationService> { FlutterArbCurrentFileModificationService(get(), get(), get(), get()) }
            single<ContextProvider> { ContextProvider() }
            single<GatherFileTranslationContext> { IdeaGatherFileTranslationContext(get(), get(), get()) }
            single<TargetLanguageProvider> { TargetLanguageProvider(get()) }
            single<TranslationClient> { GPTTranslationClient(get(), get()) }
            single<PartialFileResponseHandler> { ArbFlutterPartialFileTranslationResponseHandler(get(), get()) }
            single<TranslationRequestFactory> { TranslationRequestFactoryImpl(get()) }
            single<FileTranslationProcessController> { FileTranslationProcessController(get(), get(), get(), get(), get(), get(), get(), get()) }
            single<TargetLanguageProvider> { IdeaTargetLanguageProvider(get()) }
            single<ArbFileContentModificationService> { ArbFileContentModificationService(get()) }
            single<FinishedFileTranslationHandler> { FlutterArbTranslateFileFinished(get(), get(), get()) }
        }
    }
}
