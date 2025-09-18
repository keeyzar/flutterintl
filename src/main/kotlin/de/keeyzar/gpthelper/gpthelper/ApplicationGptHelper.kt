package de.keeyzar.gpthelper.gpthelper

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.SetupService
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.service.L10NContentService
import de.keeyzar.gpthelper.gpthelper.project.ProjectKoinService
import de.keeyzar.gpthelper.gpthelper.project.ProjectSetupStateService
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import kotlinx.coroutines.delay

/**
 * Starts the koin instance for the project
 */
class ApplicationGptHelper : ProjectActivity {
    override suspend fun execute(project: Project) {
        val instance = ProjectKoinService.getInstance(project)
        instance.start()
        val koin = instance.getKoin()
        val setupService = koin.get<SetupService>()
        val l10ContentService = koin.get<L10NContentService>()
        // wait 10s after startup before doing the check
        delay(5_000)

        // check if this looks like a flutter project by trying to read pubspec.yaml name
        val isFlutter = try {
            l10ContentService.getProjectName()
            true
        } catch (e: Exception) {
            false
        }

        if (!isFlutter) return

        val projectSetup = ProjectSetupStateService.getInstance(project)
        if (projectSetup.hasAsked()) return

        ApplicationManager.getApplication().runWriteAction {
            projectSetup.setAsked(true)
        }

        // show single notification with a "Set up?" button that opens a dialog
        val title = "Set up Flutter-Intl?"
        val content = "Hey, you want to set up flutter intl in this project? You can do it anytime later - No API key required, No AI is used here."

        val notificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("gpt-helper")
        val notification = notificationGroup?.createNotification(title, content, NotificationType.INFORMATION)

        if (notification != null) {
            notification.addAction(NotificationAction.createSimple("Set up?") {
                setupService.orchestrate()
            })
            notification.notify(project)
        }
    }
}
