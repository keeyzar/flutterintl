package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

data class UserTranslateStringRequest(
    val desiredKey: String,
    val desiredValue: String,
    val desiredDescription: String,
    /**
     * format is either en_US or en
     */
    val langsToTranslate: Set<String>,
)
