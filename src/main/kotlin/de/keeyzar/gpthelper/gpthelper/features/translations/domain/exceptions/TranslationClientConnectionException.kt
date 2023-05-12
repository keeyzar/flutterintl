package de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions

import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.GPTHelperBaseException

open class TranslationClientConnectionException : GPTHelperBaseException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
