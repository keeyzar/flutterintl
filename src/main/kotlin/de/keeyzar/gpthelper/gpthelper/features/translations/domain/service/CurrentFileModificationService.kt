package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation

/**
 * differs from [ArbFileModificationService] because this should modify the current file, e.g. often you need to add e.g. an import, change some variable names... whatever
 */
fun interface CurrentFileModificationService {
    /**
     * this is something, which should be executed immediately, e.g. when the user presses a button
     */
    fun modifyCurrentFile(translation: Translation)
}
