package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import java.security.MessageDigest

/**
 * Generates a stable ID for a string literal based on its relative file path
 * and text content. This ID is used to track which strings were previously
 * reviewed and skipped by the user.
 */
class ReviewedStringIdGenerator {

    /**
     * Generate a stable ID for a PsiElement string literal.
     * Uses SHA-256 hash of (relativeFilePath + ":" + literalText).
     */
    fun generateId(project: Project, element: PsiElement): String {
        val file = element.containingFile?.virtualFile ?: return ""
        val basePath = project.basePath ?: return ""
        val relativePath = file.path.substringAfter("$basePath/")
        val literalText = element.text
        return hash("$relativePath:$literalText")
    }

    /**
     * Generate a stable ID from path and text directly (for lookup without PSI).
     */
    fun generateId(relativeFilePath: String, literalText: String): String {
        return hash("$relativeFilePath:$literalText")
    }

    private fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}

