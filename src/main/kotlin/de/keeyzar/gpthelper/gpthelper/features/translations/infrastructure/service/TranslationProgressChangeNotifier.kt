package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

import com.intellij.util.messages.Topic
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslationProgress


interface TranslationProgressChangeNotifier {
    fun afterAction(translationProgress: TranslationProgress)

    companion object {
        val CHANGE_ACTION_TOPIC = Topic.create(
            "web browser",
            TranslationProgressChangeNotifier::class.java, Topic.BroadcastDirection.TO_PARENT
        )
    }
}
