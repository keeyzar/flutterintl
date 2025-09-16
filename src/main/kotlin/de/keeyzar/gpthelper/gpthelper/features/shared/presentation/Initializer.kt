package de.keeyzar.gpthelper.gpthelper.features.shared.presentation

import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository.FlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientConnectionTester
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.GPTModelProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationCredentialsServiceRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.project.ProjectKoinService

class Initializer(
    val userSettingsRepository: UserSettingsRepository,
    val flutterIntlSettingsRepository: FlutterIntlSettingsRepository,
    val connectionTester: ClientConnectionTester,
    val credentialsServiceRepository: TranslationCredentialsServiceRepository,
    val gptModelProvider: GPTModelProvider
) {
    companion object {
        fun create(project: Project): Initializer {
            val koin = ProjectKoinService.getInstance(project).getKoin()
            return Initializer(
                userSettingsRepository = koin.get(),
                flutterIntlSettingsRepository = koin.get(),
                connectionTester = koin.get(),
                credentialsServiceRepository = koin.get(),
                gptModelProvider = koin.get()
            )
        }
    }
}
