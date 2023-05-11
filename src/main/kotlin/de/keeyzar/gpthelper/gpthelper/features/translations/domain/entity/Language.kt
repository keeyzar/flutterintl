package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

//en_US or en
data class Language(val lang: String, val country: String?) {

    fun toISOLangString(): String {
        return if (country != null) {
            "${lang}_${country}"
        } else {
            lang
        }
    }
}
