package de.keeyzar.gpthelper.gpthelper.project

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.KoinInstances
import de.keeyzar.gpthelper.gpthelper.createAppModule
import org.koin.core.Koin

class ProjectKoinService(private val project: Project) : Disposable {

    fun start() {
        //Each project will have its own Koin context, isolated from others.
        KoinInstances.start(project, listOf(createAppModule(project)))
    }

    fun getKoin(): Koin {
        return KoinInstances.get(project)
    }

    override fun dispose() {
        // We must stop Koin to prevent memory leaks when the project is closed.
        KoinInstances.close(project)
    }

    companion object {
        fun getInstance(project: Project): ProjectKoinService {
            return project.getService(ProjectKoinService::class.java)
        }
    }
}
