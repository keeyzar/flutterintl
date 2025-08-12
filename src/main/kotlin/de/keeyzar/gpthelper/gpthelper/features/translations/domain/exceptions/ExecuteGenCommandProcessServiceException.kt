package de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions

import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.GPTHelperBaseException

/**
 * when there was an issue executing the gen command
 */
open class ExecuteGenCommandProcessServiceException : GPTHelperBaseException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
