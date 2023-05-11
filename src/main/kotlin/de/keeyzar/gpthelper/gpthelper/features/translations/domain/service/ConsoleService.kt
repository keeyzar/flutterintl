package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

/**
 *
 */
interface ConsoleService {
    /**
     * opens console, executes command, closes console
     */
    fun executeCommand(command: String)

}
