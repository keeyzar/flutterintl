package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.search

data class ArbIndexEntry(
    val key: String,
    val value: String,
    val normalizedValue: String,
    val trigrams: Set<String>
)

