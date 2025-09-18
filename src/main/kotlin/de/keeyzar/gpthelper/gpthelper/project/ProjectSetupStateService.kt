package de.keeyzar.gpthelper.gpthelper.project

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

@State(name = "GptHelperProjectSetup", storages = [Storage("gpt-helper.xml")])
class ProjectSetupStateService(private val project: Project) : PersistentStateComponent<ProjectSetupStateService.State> {

    data class State(var asked: Boolean = false)

    private var state: State? = null

    override fun getState(): State {
        if (state == null) state = State()
        return state!!
    }

    override fun loadState(state: State) {
        this.state = state
    }

    fun hasAsked(): Boolean = state?.asked ?: false

    fun setAsked(value: Boolean) {
        if (state == null) state = State()
        state!!.asked = value
    }

    companion object {
        fun getInstance(project: Project): ProjectSetupStateService {
            return project.getService(ProjectSetupStateService::class.java)
        }
    }
}
