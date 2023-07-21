package de.keeyzar.gpthelper.gpthelper.features.shared.presentation

import de.keeyzar.gpthelper.gpthelper.features.ddd.presentation.service.CreateDirectoryTreeService
import de.keeyzar.gpthelper.gpthelper.features.ddd.presentation.service.SaveDirectoryTreeService
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.repository.FlutterIntlSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.mapper.UserSettingsDTOMapper
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.ClientConnectionTester
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.client.GPTModelProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.TranslationCredentialsServiceRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.CurrentProjectProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.IdeaTranslationProgressBus
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Initializer : KoinComponent {
    val saveDirectoryTreeService: SaveDirectoryTreeService by inject();
    val createDirectoryStructureService: CreateDirectoryTreeService by inject();
    val translationPercentageBus: IdeaTranslationProgressBus by inject();
    val currentProjectProvider: CurrentProjectProvider by inject();
    val userSettingsRepository: UserSettingsRepository by inject();
    val flutterIntlSettingsRepository: FlutterIntlSettingsRepository by inject();
    val userSettingsDTOMapper: UserSettingsDTOMapper by inject()
    val connectionTester: ClientConnectionTester by inject()
    val credentialsServiceRepository: TranslationCredentialsServiceRepository by inject()
    val gptModelProvider: GPTModelProvider by inject()
}
