package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.actions.ProjectAwareAction
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer

class AutoLocalizeDirectory : ProjectAwareAction() {
    override fun actionPerformed(e: AnActionEvent, project: Project, initializer: FlutterArbTranslationInitializer) {
        val directory = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        initializer.orchestrator.orchestrate(project, directory, "Auto Localizing Directory")
    }
}
