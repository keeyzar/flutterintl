package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.client

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import groovy.util.logging.Slf4j
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import java.util.concurrent.Executors

@Slf4j
class DispatcherConfiguration(
    private val userSettingsRepository: UserSettingsRepository,
) {
    private var dispatcher: ExecutorCoroutineDispatcher? = null

    //default level of parallelism - because openAI lets you only make 3 calls in parallel at account creation for some days
    private var levelOfParallelism: Int = 3
    private var dirty = false

    fun getDispatcher(): ExecutorCoroutineDispatcher {
        checkIfParallelismChanged()
        closeDispatcherIfRequired()
        var tmpDispatcher = this.dispatcher
        if (tmpDispatcher == null) {
            tmpDispatcher = Executors.newFixedThreadPool(levelOfParallelism).asCoroutineDispatcher();
            this.dispatcher = tmpDispatcher
            return tmpDispatcher
        }
        return tmpDispatcher;
    }

    fun getLevelOfParallelism(): Int {
        return levelOfParallelism
    }

    private fun closeDispatcherIfRequired() {
        if (dirty) {
            dispatcher?.close()
            dispatcher = null
            dirty = false
        }
    }

    private fun checkIfParallelismChanged() {
        val newParallelism = userSettingsRepository.getSettings().parallelism
        if (newParallelism != levelOfParallelism) {
            println("parallelism changed from $levelOfParallelism to $newParallelism, creating new dispatcher")
            levelOfParallelism = newParallelism
            dirty = true
        }
    }
}
