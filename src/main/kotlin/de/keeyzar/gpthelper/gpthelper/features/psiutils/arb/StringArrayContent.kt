package de.keeyzar.gpthelper.gpthelper.features.psiutils.arb

/**
 * Often used in arb intl files
 * e.g. "de" -> "key1", "key2"
 */
data class StringArrayContent(
    val key: String,
    val values: List<String>,
)