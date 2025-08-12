package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client

/**
 * this is a simple Best Guess Response.
 * There is also some more complex l10n possibilities, but let's ignore them for now
 */
data class BestGuessResponse(
    val responseEntries: List<BestGuessResponseEntry>
)
