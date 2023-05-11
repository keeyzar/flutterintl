package de.keeyzar.gpthelper.gpthelper.features.translations.domain.mapper

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientTranslationRequest
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationRequest
import org.mapstruct.Mapper
import org.mapstruct.Mapping

/**
 * map from UserTranslationRequest to ClientTranslationRequest
 */
@Mapper
interface TranslationRequestMapper {
    @Mapping(source = "baseTranslation", target = "translation")
    fun toClientRequest(userTranslationRequest: UserTranslationRequest): ClientTranslationRequest
    @Mapping(source = "translation", target = "baseTranslation")
    fun toUserRequest(clientTranslationRequest: ClientTranslationRequest): UserTranslationRequest
}
