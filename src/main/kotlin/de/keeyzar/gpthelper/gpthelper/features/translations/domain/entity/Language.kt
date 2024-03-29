package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

//en_US or en
data class Language(val lang: String, val country: String?) {
    companion object {
        fun fromISOLangString(isoLang: String): Language {
            val split = isoLang.split("_")
            return if (split.size == 2) {
                Language(split[0], split[1])
            } else {
                Language(split[0], null)
            }
        }
    }


    fun toISOLangString(): String {
        return if (country != null) {
            "${lang}_${country}"
        } else {
            lang
        }
    }
}
