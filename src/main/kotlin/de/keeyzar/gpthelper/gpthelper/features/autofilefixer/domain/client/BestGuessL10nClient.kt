package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client

/**
 * will provide best guesses for translation keys, so the user does not have to type so many manually
 */
interface BestGuessL10nClient {
    /**
     * provides, based on context, l10n keys and descriptions
     * simple in context of the l10n type, i.e. you could have a more complex type having parameters for example
     */
    suspend fun simpleGuess(bestGuessRequest: BestGuessRequest, progressReport: (() -> Unit)? = null): BestGuessResponse
}
