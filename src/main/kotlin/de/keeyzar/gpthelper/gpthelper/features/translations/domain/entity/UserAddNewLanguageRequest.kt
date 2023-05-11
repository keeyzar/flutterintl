package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

data class UserAddNewLanguageRequest(
    val oldFileContent: String,
    // format is either en_US or en
    val targetLanguage: String,
)
