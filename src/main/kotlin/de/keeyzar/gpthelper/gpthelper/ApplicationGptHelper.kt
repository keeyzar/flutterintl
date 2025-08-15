package de.keeyzar.gpthelper.gpthelper

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import de.keeyzar.gpthelper.gpthelper.DIConfig.Companion.appModule
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.Initializer
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin


class ApplicationGptHelper : ProjectActivity {
    override suspend fun execute(project: Project) {
        //ensure koin is not started twice
        if (GlobalContext.getOrNull() == null) {
            startKoin {
                modules(appModule)
            }
            val initializer = Initializer()
            initializer.translationPercentageBus.init(project)
            initializer.currentProjectProvider.project = project
        }
    }
}
