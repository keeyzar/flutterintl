package de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language
import java.nio.file.Path

/**
 * different parsing functions for arb filenames, based from different points of view
 */
class ArbFilenameParser {
    fun stringToLanguage(langAsString: String): Language {
        //it's either en_US or en / de_DE or de / ...
        val split = langAsString.split("_")
        return if (split.size == 1) {
            Language(split[0], null)
        } else {
            Language(split[0], split[1])
        }
    }

    /**
     * returns the prefix of the arb file, either finds en_US or en in a file named app_en_US.arb / app_en.arb
     */
    fun getArbFilePrefix(filename: String): String {
        val regex = Regex("^(.*?)([a-z]{2}(?:_[A-Z]{2})?)?(\\.arb)$")
        val matchResult = regex.matchEntire(filename)
        return matchResult?.groups?.get(1)?.value ?: ""
    }

    fun getLanguageFromFilename(filename: String): Language {
        val languageString = filename.replace(getArbFilePrefix(filename), "").replace(".arb", "")
        return stringToLanguage(languageString)
    }
    fun getLanguageFromPath(filePath: Path): Language {
        val filename = filePath.fileName.toString()
        val languageString = filename.replace(getArbFilePrefix(filename), "").replace(".arb", "")
        return stringToLanguage(languageString)
    }
}
