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

    /**
     * Presents multiple proposed file modifications (original vs modified) and allows the user
     * to select which referenced items should be applied. Returns the list of selected references
     * (the original "reference" objects) or null if the user cancelled.
     */
    fun confirmProjectFileModifications(changes: List<ProjectFileChange>): List<Any>?
}

/** Represents a single file/reference change (original and modified content). */
data class ProjectFileChange(
    val reference: Any,
    val original: String,
    val modified: String
)
