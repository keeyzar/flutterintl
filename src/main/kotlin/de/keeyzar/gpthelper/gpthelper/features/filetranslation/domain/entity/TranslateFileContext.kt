package de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import java.nio.file.Path

/**
 * contains information about the current request to translate a key,
 * though from the perspective of us, asking the user for information
 *
 */
data class TranslateFileContext(
    /**
     * the current statement to be translated
     * most of the time a StringLiteral, but might be something different in the future
     */
    val baseLanguage: Language,
    val targetLanguage: Language,
    val absolutePath: Path,
) {
}
