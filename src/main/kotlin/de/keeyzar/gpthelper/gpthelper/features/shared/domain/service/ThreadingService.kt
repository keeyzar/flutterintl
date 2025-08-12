package de.keeyzar.gpthelper.gpthelper.features.shared.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.util.concurrent.LinkedBlockingQueue

class ThreadingService<T>(
    private val queue: LinkedBlockingQueue<T> = LinkedBlockingQueue(),
    private var initialized: Boolean = false,
) {

    suspend fun putIntoQueue(elem: T) {
        withContext(Dispatchers.IO) {
            queue.put(elem)
        }
    }

    fun startQueueIfNotRunning(handler: suspend (T) -> Unit) {
        if (initialized) {
            return
        }
        initialized = true

        // Create a background thread to process the queue
        val backgroundThread = Thread {
            while (true) {
                val item = queue.take() // Blocks until an item is available
                runBlocking {
                    //we run in blocking context, because it's not required to run in parallel. it's fine to run sequentially,
                    //but we might do so in the future
                    handler.invoke(item)
                }
            }
        }
        backgroundThread.start()
    }
}