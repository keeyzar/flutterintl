package de.keeyzar.gpthelper.gpthelper.features.ddd.presentation.service

import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import de.keeyzar.gpthelper.gpthelper.features.ddd.domain.repository.DDDSettingsRepository

private const val DEFAULT_KEY = "default"

class CreateDirectoryTreeService(private val repository: DDDSettingsRepository) {
    fun createDirectoryTree(projectName: String, rootDir: VirtualFile): List<VirtualFile>? {
        var directoryStructure = repository.getDirectoryStructure(projectName) ?: return null

        val baseDir = LocalFileSystem.getInstance().refreshAndFindFileByPath(rootDir.path) ?: return null
        var currentDir: VirtualFile = baseDir

        val createdDirs = mutableListOf<VirtualFile>()

        for (directoryPath in directoryStructure.subDirectories) {
            val pathParts = directoryPath.split("/")
            for (pathPart in pathParts) {
                if (pathPart.isBlank()) continue
                val child = currentDir.findChild(pathPart)
                    ?: currentDir.createChildDirectory(this, pathPart.removePrefix("/"))
                currentDir = child
            }
            createdDirs.add(currentDir)
            currentDir = baseDir // Reset the current directory to the base directory
        }

        return createdDirs
    }
}
