package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.ExecuteGenCommandProcessServiceException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.ConsoleService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.ExternalTranslationProcessService

/**
 * implements the domain service for the external translation process
 */
class FlutterGenCommandProcessService(
    private val ideaTerminalConsoleService: ConsoleService,
) : ExternalTranslationProcessService {
    companion object {
        const val FLUTTER_GEN_COMMAND = "flutter gen-l10n"
    }
    override fun postTranslationProcess() {
        try {
            ideaTerminalConsoleService.executeCommand(FLUTTER_GEN_COMMAND)
        } catch (e: Exception) {
            throw ExecuteGenCommandProcessServiceException("Could not execute '$FLUTTER_GEN_COMMAND' in console. You might need to do it yourself. Is there any terminal tab open?", e)
        }
    }
}
