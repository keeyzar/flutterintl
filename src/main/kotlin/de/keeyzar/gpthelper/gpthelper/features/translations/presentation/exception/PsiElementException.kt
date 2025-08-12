package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.exception

import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.GPTHelperBaseException

/**
 * when there was some issue with the statements
 */
class PsiElementException : GPTHelperBaseException {
    constructor() : super()
    constructor(message: String?) : super(message)
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
