package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessResponse
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.MultiKeyTranslationContext
import java.util.*

/**
 * we have a lot of guesses now, that must be checked and modified (by the user most definitely)
 */
fun interface GuessAdaptionService {
    /**
     * the user now has a lot of possible translation entries, we need to allow him to modify them
     * afterward we take all these l10n translation requests and exchange them in the file
     *
     * @return null, when the user has interrupted the process
     */
    fun adaptBestGuess(processUUID: UUID, bestGuessResponse: BestGuessResponse): MultiKeyTranslationContext?
}
