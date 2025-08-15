package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.DumbAwareAction
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer

class AutoLocalizeDirectory : DumbAwareAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project!!
        val directory = event.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val initializer = FlutterArbTranslationInitializer()

        initializer.orchestrator.orchestrate(project, directory, "Auto Localizing Directory")
    }

}
