package de.keeyzar.gpthelper.gpthelper.features.ddd.domain.repository

import de.keeyzar.gpthelper.gpthelper.features.ddd.domain.entity.DirectoryStructure

interface DDDSettingsRepository {
    fun saveDirectoryStructure(projectName: String, directoryStructure: DirectoryStructure)
    fun getDirectoryStructure(projectName: String): DirectoryStructure?
}
