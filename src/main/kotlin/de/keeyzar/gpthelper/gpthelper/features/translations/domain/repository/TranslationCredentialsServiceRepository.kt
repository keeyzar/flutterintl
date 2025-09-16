package de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository

interface TranslationCredentialsServiceRepository {
    fun persistKey(key: String)
    fun getKey(): String?
    fun hasPassword(): Boolean
}
