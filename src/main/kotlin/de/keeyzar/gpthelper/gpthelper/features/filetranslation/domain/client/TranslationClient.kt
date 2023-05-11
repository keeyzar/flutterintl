package de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client

fun interface TranslationClient {
    /**
     * request the translation of a file
     */
    suspend fun translate(fileTranslationRequest: FileTranslationRequest, partialTranslationFinishedCallback: (PartialFileTranslationResponse, leftTaskSize: Int) -> Unit)
}
