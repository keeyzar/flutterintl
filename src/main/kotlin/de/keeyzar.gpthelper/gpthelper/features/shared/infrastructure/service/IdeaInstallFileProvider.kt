package de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.service

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.InstallFileProvider
import java.io.IOException

class IdeaInstallFileProvider(private val project: Project) : InstallFileProvider {
    override fun fileExists(path: String): Boolean {
        return LocalFileSystem.getInstance().findFileByPath(path)?.exists() ?: false
    }

    override fun readFile(path: String): String? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        return try {
            VfsUtil.loadText(virtualFile)
        } catch (e: IOException) {
            null
        }
    }

    override fun writeFile(path: String, content: String) {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                VfsUtil.saveText(virtualFile, content)
            } catch (e: IOException) {
                // Handle exception, e.g., log it
            }
        }
    }
}
