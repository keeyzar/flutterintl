package de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.service

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.InstallFileProvider
import java.io.IOException
import java.nio.file.Paths

class IdeaInstallFileProvider(private val project: Project) : InstallFileProvider {
    override fun fileExists(path: String): Boolean {
            return LocalFileSystem.getInstance().findFileByPath(path)?.exists() ?: false
    }

    override fun readFile(path: String): String? {
        val virtualFile = LocalFileSystem.getInstance().findFileByPath(path) ?: return null
        return try {
            VfsUtil.loadText(virtualFile)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    override fun writeFile(path: String, content: String) {
        print("Writing $path")
        var virtualFile = LocalFileSystem.getInstance().findFileByPath(path)
        if (virtualFile == null) {
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    val parentPath = Paths.get(path).parent.toString()
                    val fileName = Paths.get(path).fileName.toString()
                    val parentDir = VfsUtil.createDirectoryIfMissing(parentPath)
                    virtualFile = parentDir!!.createChildData(this, fileName)
                    virtualFile.setBinaryContent(content.toByteArray(Charsets.UTF_8))
                    print("Success writing at ${virtualFile.path}")
                } catch (e: IOException) {
                    e.printStackTrace()
                    // Handle exception, e.g., log it
                }
            }
        } else {
            WriteCommandAction.runWriteCommandAction(project) {
                try {
                    virtualFile.setBinaryContent(content.toByteArray(Charsets.UTF_8))
                    print("Success writing at ${virtualFile.path}")
                } catch (e: IOException) {
                    e.printStackTrace()
                    // Handle exception, e.g., log it
                }
            }
        }
    }
}
