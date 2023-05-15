package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.model.AutoLocalizeContext
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer
import java.util.*


class AutoLocalizeFile : DumbAwareAction() {
    override fun actionPerformed(event: AnActionEvent) {
        val initializer = FlutterArbTranslationInitializer()
        val uuid = UUID.randomUUID()
        val project = event.project!!
        initializer.contextProvider.putAutoLocalizeContext(uuid, AutoLocalizeContext(project, event.getData(PlatformDataKeys.PSI_FILE)!!))
        //ah well, need to push that to the background here
        initializer.translationTaskBackgroundProgress.triggerInBlockingContext(project,
            {
                initializer.bestGuessProcessController.startBestGuessProcess(uuid)
            },
            {
                //TODO wow, I need to clean up the whole currentProvider thingy, because I need
                initializer.psiElementIdReferenceProvider.deleteAllElements()
                initializer.contextProvider.removeAutoLocalizeContext(uuid)
            }
        )
    }
}
