package de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.service

import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.PathProvider

/**
 * IntelliJ IDEA implementation of the PathProvider.
 * For this example, it returns a dummy path.
 */
class IdeaPathProvider(val project: Project) : PathProvider {
    override fun getRootPath(): String {
        return project.basePath!!
    }
}

