package de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.*


/**
 * parses the user translation input
 */
class UserTranslationInputParser(
    private val parser: ArbFilenameParser,
) {
    fun toUserTranslationRequest(baseLanguage: Language, userTranslationInput: UserTranslationInput): UserTranslationRequest {
        return UserTranslationRequest(
            targetLanguages = toTargetLanguages(userTranslationInput),
            baseTranslation = toTranslation(baseLanguage, userTranslationInput)
        );
    }

    private fun toTargetLanguages(userTranslationInput: UserTranslationInput) =
        userTranslationInput.languagesToTranslate
            .filter { it.value }
            .map { parser.stringToLanguage(it.key) }

    private fun toTranslation(baseLanguage: Language, userTranslationInput: UserTranslationInput): Translation {
        return Translation(
            lang = baseLanguage,
            entry = SimpleTranslationEntry(
                //TODO WHERE DO I GET THE ID FROM?
                id = null,
                desiredKey = userTranslationInput.desiredKey,
                desiredValue = userTranslationInput.desiredValue,
                desiredDescription = userTranslationInput.desiredDescription,
            )
        );
    }

}
