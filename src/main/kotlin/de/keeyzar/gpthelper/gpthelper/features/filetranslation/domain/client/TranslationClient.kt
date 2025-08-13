package de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.client

/***
 * is used only in context of file translation - may be removed
 */
fun interface TranslationClient {
    /**
     * request the translation of a file
     */
    suspend fun translate(fileTranslationRequest: FileTranslationRequest, partialTranslationFinishedCallback: (PartialFileTranslationResponse, leftTaskSize: Int) -> Unit)
}
