package de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.client

import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.FileTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.PartialFileTranslationResponse
import de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client.TranslationClient
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.SingleTranslationRequestClient
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.JsonFileChunker
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

class GPTTranslationClient(
    private val jsonFileChunker: JsonFileChunker,
    private val singleTranslationRequestClient: SingleTranslationRequestClient,
) : TranslationClient {
    override suspend fun translate(
        fileTranslationRequest: FileTranslationRequest,
        partialTranslationFinishedCallback: (PartialFileTranslationResponse, taskSize: Int) -> Unit
    ) {
        val chunkedStrings = jsonFileChunker.chunkJsonBasedOnTotalStringSize(fileTranslationRequest.content, 500);
        // request translation for each chunk in parallel
        //not sure, whether this will block the caller
        val requestSemaphore = Semaphore(9)
        coroutineScope {
            return@coroutineScope chunkedStrings.map {
                async {
                    requestSemaphore.withPermit {
                        val response = singleTranslationRequestClient.requestTranslation(it, fileTranslationRequest.baseLanguage, fileTranslationRequest.targetLanguage)
                        partialTranslationFinishedCallback(PartialFileTranslationResponse(response.content), chunkedStrings.size+1)
                    }
                }
            }
        }
    }
}
