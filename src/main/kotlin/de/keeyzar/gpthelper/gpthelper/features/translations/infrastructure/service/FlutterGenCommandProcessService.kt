package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.ExecuteGenCommandProcessServiceException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.ExternalTranslationProcessService
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.IdeaTerminalConsoleService

/**
 * implements the domain service for the external translation process
 */
class FlutterGenCommandProcessService(
    private val ideaTerminalConsoleService: IdeaTerminalConsoleService,
    private val project: Project,
) : ExternalTranslationProcessService {
    companion object {
        const val FLUTTER_GEN_COMMAND = "flutter gen-l10n"
    }
    override fun postTranslationProcess() {
        try {
            ideaTerminalConsoleService.executeCommand(project, FLUTTER_GEN_COMMAND)
        } catch (e: Exception) {
            throw ExecuteGenCommandProcessServiceException("Could not execute '$FLUTTER_GEN_COMMAND' in console. You might need to do it yourself. Is there any terminal tab open?", e)
        }
    }
}
