package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.ExecuteGenCommandProcessServiceException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.ExternalTranslationProcessService
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.CurrentProjectProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.IdeaTerminalConsoleService

/**
 * implements the domain service for the external translation process
 */
class FlutterGenCommandProcessService(
    private val ideaTerminalConsoleService: IdeaTerminalConsoleService,
    private val currentProjectProvider: CurrentProjectProvider,
) : ExternalTranslationProcessService {
    companion object {
        const val FLUTTER_GEN_COMMAND = "flutter gen-l10n"
    }
    override fun postTranslationProcess() {
        //should be obsolete in some time
        try {
            ideaTerminalConsoleService.executeCommand(currentProjectProvider.project, FLUTTER_GEN_COMMAND)
        } catch (e: Exception) {
            throw ExecuteGenCommandProcessServiceException("Could not execute '$FLUTTER_GEN_COMMAND' in console. You might need to do it yourself. Is there any terminal tab open?", e)
        }
    }
}
