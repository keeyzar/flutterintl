package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client

/**
 * this is a simple Best Guess Response.
 * There is also some more complex l10n possibilities, but let's ignore them for now
 */
data class BestGuessResponseEntry(
    val id: String,
    val key: String,
    val description: String,
    val placeholder: Map<String, *>? = null
)
