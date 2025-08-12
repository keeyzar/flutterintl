package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.exception

import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.GPTHelperBaseException

open class BestGuessClientException : GPTHelperBaseException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
