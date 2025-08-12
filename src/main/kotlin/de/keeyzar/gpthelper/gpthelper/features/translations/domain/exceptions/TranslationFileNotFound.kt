package de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions

import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.GPTHelperBaseException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import java.nio.file.Path

class TranslationFileNotFound(message: String, private var language: Language, private var targetPath: Path) : GPTHelperBaseException(message) {

    override val message: String = ""
        get() = "Target file not found for language '${language.toISOLangString()}'. File path searched: '$targetPath' + $field"
}
