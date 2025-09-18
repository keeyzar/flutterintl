package de.keeyzar.gpthelper.gpthelper.features.setup.domain.service

/**
 * Defines the dialogs shown to the user during the setup process.
 */
interface UserInstallDialogs {
    fun showDiff(title: String, before: String, after: String): String?
    fun confirmLibraryInstallation(diffContent: String): String?
    fun confirmL10nConfiguration(config: String): Boolean
    fun confirmProjectFileModification(diffContent: String): String?
    fun selectAppFile(files: List<String>): String?
    fun showInfo(title: String, message: String)
}
