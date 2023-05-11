package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.SingleTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.ARBFileContent
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserAddNewLanguageRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslateStringRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.TranslationProgressBus
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.TranslationService
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.parser.ARBFileContentParser
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.util.concurrent.atomic.AtomicInteger

class GPTTranslationService(
    private val singleTranslationRequestClient: SingleTranslationRequestClient,
    private val arbFileContentParser: ARBFileContentParser,
    private val immediateTranslationService: ImmediateTranslationService,
    private val jsonFileChunker: JsonFileChunker,
    private val jsonFileChunkMerger: JsonChunkMerger,
    private val translationProgressBus: TranslationProgressBus,
) : TranslationService {
    override suspend fun translate(userTranslateStringRequest: UserTranslateStringRequest): Set<ARBFileContent> {
        val (
            key: String,
            value: String,
            description: String,
            languages: Set<String>,
        ) = userTranslateStringRequest


        translationProgressBus.pushPercentage(TranslationProgress(languages.size, 0))
        val requestSemaphore = Semaphore(3);
        val counter = AtomicInteger(0)
        return coroutineScope {
            return@coroutineScope languages.map {
                async {
                    requestSemaphore.withPermit {
                        val requestTranslation = singleTranslationRequestClient.requestTranslation(key, value, description, it)
                        val arbFileContent = arbFileContentParser.toARB(it, requestTranslation)
                        translationProgressBus.pushPercentage(TranslationProgress(languages.size, counter.incrementAndGet()))
                        return@async arbFileContent
                    }
                }
            }.awaitAll().toSet()
        }
    }

    override fun placeholderTranslate(userTranslateStringRequest: UserTranslateStringRequest): Set<ARBFileContent> {
        val (
            key: String,
            value: String,
            description: String,
            languages: Set<String>,
        ) = userTranslateStringRequest

        return languages.map {
            val translation = immediateTranslationService.requestTranslation(key, value, description)
            return@map arbFileContentParser.toARB(it, translation)
        }.toSet()
    }

    override suspend fun translateWholeFile(userAddNewLanguageRequest: UserAddNewLanguageRequest): String {
        val chunkedStrings = jsonFileChunker.chunkJson(userAddNewLanguageRequest.oldFileContent, 4);
        // request translation for each chunk in parallel
        val requestSemaphore = Semaphore(3);
        val counter = AtomicInteger(0)
        translationProgressBus.pushPercentage(TranslationProgress(chunkedStrings.size, 0))
        val translatedChunks = coroutineScope {
            return@coroutineScope chunkedStrings.map {
                async {
                    requestSemaphore.withPermit {
                        val x = singleTranslationRequestClient.requestTranslation(it, userAddNewLanguageRequest.targetLanguage)
                        val newFileContent = arbFileContentParser.toARB(it, x)
                            .content
                        translationProgressBus.pushPercentage(TranslationProgress(chunkedStrings.size, counter.incrementAndGet()))

                        return@async newFileContent
                    }
                }
            }.awaitAll();
        }
        // merge all chunks
        return jsonFileChunkMerger.mergeChunks(translatedChunks);
    }
}

