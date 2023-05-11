package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationInput

/**
 * get information from the user, like the desired key, description, ...
 */
fun interface GatherUserInputService {
    /**
     * e.g. get the data from the user, or calculate it based on the class, whatever
     * @param translateKeyContext is information, which helps the process of getting the information from the user
     */
    fun requestInformationFromUser(translateKeyContext: TranslateKeyContext): UserTranslationInput?
}
