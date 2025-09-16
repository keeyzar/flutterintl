package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import com.intellij.openapi.project.Project
import com.intellij.util.messages.MessageBus
import com.intellij.util.messages.MessageBusConnection
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.TranslationProgressBus

class IdeaTranslationProgressBus(
    val project: Project
) : TranslationProgressBus {

    override fun pushPercentage(translationProgress: TranslationProgress) {
        val messageBus: MessageBus = project.messageBus
        val connection: MessageBusConnection = messageBus.connect()
        messageBus.syncPublisher(TranslationProgressChangeNotifier.CHANGE_ACTION_TOPIC)
            .afterAction(translationProgress)
        connection.disconnect()
    }
}
