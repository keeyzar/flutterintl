package de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity

/**
 * is the representation of a file in our domain
 */
data class FileToTranslate(
    /**
     * the language of the file
     */
    val language: Language,
    /**
     * the content of the file, e.g. arb is JSON, but there might be other formats
     * might theoretically be a simple translation entry
     */
    val content: String,
) {
}
