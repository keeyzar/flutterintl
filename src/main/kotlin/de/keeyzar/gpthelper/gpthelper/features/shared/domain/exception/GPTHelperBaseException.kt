package de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception

open class GPTHelperBaseException : Throwable {
    constructor(message: String?, cause: Throwable?) : super(message, cause)
    constructor(message: String?) : super(message)
    constructor(cause: Throwable?) : super(cause)
    constructor() : super()
    constructor(message: String?, cause: Throwable?, enableSuppression: Boolean, writableStackTrace: Boolean) : super(
        message,
        cause,
        enableSuppression,
        writableStackTrace
    )
}
