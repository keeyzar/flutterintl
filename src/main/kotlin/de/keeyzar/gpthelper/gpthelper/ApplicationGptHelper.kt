package de.keeyzar.gpthelper.gpthelper

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import de.keeyzar.gpthelper.gpthelper.project.ProjectKoinService

/**
 * Starts the koin instance for the project
 */
class ApplicationGptHelper : ProjectActivity {
    override suspend fun execute(project: Project) {
        ProjectKoinService.getInstance(project).start()
    }
}
