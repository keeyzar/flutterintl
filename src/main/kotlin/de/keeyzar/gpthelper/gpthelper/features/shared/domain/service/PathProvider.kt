package de.keeyzar.gpthelper.gpthelper.features.shared.domain.service

/**
 * Provides paths for the current project.
 */
interface PathProvider {
    /**
     * Returns the root path of the project.
     */
    fun getRootPath(): String
}

