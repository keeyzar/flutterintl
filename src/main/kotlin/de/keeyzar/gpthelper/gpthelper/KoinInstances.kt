package de.keeyzar.gpthelper.gpthelper

import com.intellij.openapi.project.Project
import org.koin.core.Koin
import org.koin.core.KoinApplication
import org.koin.core.module.Module
import org.koin.dsl.koinApplication

object KoinInstances {
    private val projectKoinApps = mutableMapOf<Project, KoinApplication>()

    fun start(project: Project, modules: List<Module>) {
        println("Starting Koin for project ${project.name}")
        val koinApp = koinApplication {
            modules(modules)
        }
        projectKoinApps[project] = koinApp
    }

    fun get(project: Project): Koin {
        return projectKoinApps[project]?.koin ?: error("Koin not started for project ${project.name}")
    }

    fun close(project: Project) {
        projectKoinApps.remove(project)?.close()
        println("Closed Koin for project ${project.name}")
    }
}
