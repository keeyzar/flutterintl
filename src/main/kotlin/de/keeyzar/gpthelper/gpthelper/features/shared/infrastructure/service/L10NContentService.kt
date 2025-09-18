package de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.service

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import java.nio.file.Paths

class L10NContentService(private val project: Project) {


    fun getProjectName(): String {
        val basePath = project.basePath ?: throw IllegalStateException("Project base path is null")
        val pubspecPath = Paths.get(basePath, "pubspec.yaml")
        val pubspecFile =
            VfsUtil.findFile(pubspecPath, true) ?: throw IllegalStateException("pubspec.yaml not found at $pubspecPath")
        val psiFile = PsiManager.getInstance(project).findFile(pubspecFile) as? YAMLFile
            ?: throw IllegalStateException("Could not parse pubspec.yaml as YAML file.")

        return PsiTreeUtil.findChildrenOfType(psiFile, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == "name" }
            ?.valueText
            ?: throw IllegalStateException("Could not find 'name' in pubspec.yaml")
    }

    fun getArbDir(): String {
        return getL10nYamlValue("arb-dir") ?: "lib/l10n"
    }

    fun getOutputLocalizationFile(): String {
        return (getL10nYamlValue("output-localization-file") ?: "app_localizations")
    }


    fun getOutputClass(): String {
        return (getL10nYamlValue("output-class") ?: "AppLocalizations")
    }

    fun getNullableGetter(): Boolean {
        return getL10nYamlValue("nullable-getter")?.toBoolean() ?: true
    }

    // when e.g. lib/l10n is generated, we return l10n (remove leading lib/)
    fun getPath(): String {
        val arbDir = getArbDir()
        return arbDir.removePrefix("lib/")
    }

    private fun getL10nYamlValue(key: String): String? {
        val basePath = project.basePath ?: return null
        val l10nPath = Paths.get(basePath, "l10n.yaml")
        val l10nVirtual = VfsUtil.findFile(l10nPath, true) ?: return null
        val psiFile = PsiManager.getInstance(project).findFile(l10nVirtual) as? YAMLFile ?: return null

        return PsiTreeUtil.findChildrenOfType(psiFile, YAMLKeyValue::class.java)
            .firstOrNull { it.keyText == key }
            ?.valueText
    }

}