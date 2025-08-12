package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity

data class FileBestGuessContext(
    val filename: String,
    val literals: List<SingleLiteral>
)
