package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository

import com.intellij.openapi.project.Project

/**
 * only used in infrastructure and presentation, because we need to encapsulate the "project" thing
 * of the IDE
 */
class CurrentProjectProvider() {
    lateinit var project: Project
}
