package de.keeyzar.gpthelper.gpthelper

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity.DumbAware
import de.keeyzar.gpthelper.gpthelper.DIConfig.Companion.appModule
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.Initializer
import org.koin.core.context.startKoin


class ApplicationGptHelper() : DumbAware {
    override fun runActivity(project: Project) {
        startKoin() {
            modules(appModule)
        }
        val initializer = Initializer()
        initializer.translationPercentageBus.init(project)
        initializer.currentProjectProvider.project = project

    }
}
