package de.keeyzar.gpthelper.gpthelper.features.shared.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer

abstract class ProjectAwareAction : DumbAwareAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val initializer = FlutterArbTranslationInitializer.create(project)
        actionPerformed(e, project, initializer)
    }

    abstract fun actionPerformed(e: AnActionEvent, project: Project, initializer: FlutterArbTranslationInitializer)
}
