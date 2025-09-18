package de.keeyzar.gpthelper.gpthelper

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.SetupService
import de.keeyzar.gpthelper.gpthelper.project.ProjectKoinService

/**
 * Starts the koin instance for the project
 */
class ApplicationGptHelper : ProjectActivity {
    override suspend fun execute(project: Project) {
        val instance = ProjectKoinService.getInstance(project)
        instance.start()
//        val koin = instance.getKoin()
//        val setupService = koin.get<SetupService>()
//        //TODO check if is installed, if not, add a notification to bottom right
//        //whether the user wants to install (we need to check if this is a flutter project)
//        //the whole application can be disabled, if it's not flutter
//        setupService.needsSetup()
    }
}
