package de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions

import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.GPTHelperBaseException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation

class TranslationRequestException(message: String, cause: Throwable, private var targetTranslation: Translation) : GPTHelperBaseException(message, cause) {
}
