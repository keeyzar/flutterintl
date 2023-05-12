package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationCredentialsServiceRepository


class IdeaTranslationCredentialsServiceRepository : TranslationCredentialsServiceRepository {

    override fun persistKey(key: String) {
        val credentialAttributes = createCredentialAttributes()
        PasswordSafe.instance.set(credentialAttributes, Credentials("", key))
    }

    override fun getKey(): String? {
        val credentialAttributes = createCredentialAttributes()
        return PasswordSafe.instance.get(credentialAttributes)?.getPasswordAsString()
    }

    private fun createCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(
            serviceName = generateServiceName("openai", "apikey"),
        )
    }

}
