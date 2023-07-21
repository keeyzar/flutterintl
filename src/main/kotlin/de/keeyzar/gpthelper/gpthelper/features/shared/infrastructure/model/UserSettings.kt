package de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model

data class UserSettings(
    /**
     * where is the directory containing the arb files
     */
    val arbDir: String?,
    /**
     * I might need to remove the key here, because it should not be visible everywhere, where we need access to the userSettings
     */
    val outputClass: String,
    /**
     * whether we're working with a nullable getter
     */
    val nullableGetter: Boolean,

    /**
     * base file for the translations, i.e. here you can find the prefix
     */
    val templateArbFile: String,
    val intlConfigFile: String?,
    val watchIntlConfigFile: Boolean = true,
    /**
     * here you can find the file, where the outputClass is located in
     */
    val outputLocalizationFile: String,
    /**
     * how many requests should be done in parallel
     */
    val parallelism: Int,
    /**
     * the tonality of the voice (i.e. formal, informal)
     */
    val tonality: String,
    /**
     * which model should be used?
     */
    val gptModel: String,
    /**
     * whether to use the new advanced arb key translation
     */
    val translateAdvancedArbKeys: Boolean
) {

    fun getArbFilePrefix(): String {
        val regex = Regex("^(.*?)([a-z]{2}(?:_[A-Z]{2})?)?(\\.arb)$")
        val matchResult = regex.matchEntire(templateArbFile)
        return matchResult?.groups?.get(1)?.value ?: ""
    }
}
