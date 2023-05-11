package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.pojo

data class TranslationDialogUserInput(
    val translationsChecked: MutableMap<String, Boolean>,
    var desiredValue: String,
    var desiredKey: String,
    var desiredDescription: String,

)
