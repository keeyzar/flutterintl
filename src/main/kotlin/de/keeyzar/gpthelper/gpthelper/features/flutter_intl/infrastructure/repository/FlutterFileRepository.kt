package de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.repository

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.domain.exceptions.FlutterIntlFileNotFound
import java.nio.file.Path

/**
 * get the file, externalized for testing
 */
class FlutterFileRepository {
    fun getFileContent(project: Project, filePath: Path): String {
        return ReadAction.compute<String, Throwable> {
            val virtualFile = VirtualFileManager.getInstance().findFileByNioPath(filePath)
                ?: throw FlutterIntlFileNotFound("File not found at $filePath")
            val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                ?: throw FlutterIntlFileNotFound("We found a file, but it might be a directory or not a valid file at $filePath")
            return@compute PsiDocumentManager.getInstance(project).getDocument(psiFile)?.text
                ?: throw FlutterIntlFileNotFound("File found, but could not proceed, because the file is e.g. binary or there is no associated document at $filePath")
        }
    }
}
