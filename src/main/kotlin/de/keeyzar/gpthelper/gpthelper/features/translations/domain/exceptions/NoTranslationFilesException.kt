package de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions

import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.GPTHelperBaseException

/**
 * one could theoretically ask the user to create translation files
 */
open class NoTranslationFilesException : GPTHelperBaseException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
