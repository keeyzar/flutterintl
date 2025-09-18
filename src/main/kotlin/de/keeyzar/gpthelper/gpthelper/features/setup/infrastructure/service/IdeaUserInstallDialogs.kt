package de.keeyzar.gpthelper.gpthelper.features.setup.infrastructure.service

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.UserInstallDialogs
import org.jetbrains.yaml.YAMLFileType
import javax.swing.JComponent

/**
 * IntelliJ IDEA implementation of the UserInstallDialogs that shows a real diff viewer.
 */
class IdeaUserInstallDialogs(private val project: Project) : UserInstallDialogs {
    override fun showDiff(title: String, before: String, after: String): String? {
        val diffContentFactory = DiffContentFactory.getInstance()
        val content1 = diffContentFactory.create(before)
        val content2 = diffContentFactory.createEditable(project, after, YAMLFileType.YML)
        val request = SimpleDiffRequest(title, content1, content2, "Before", "After")

        val diffViewer = DiffManager.getInstance().createRequestPanel(project, {}, null)
        diffViewer.setRequest(request)

        val dialog = object : DialogWrapper(project) {
            init {
                init()
                setTitle(title)
            }

            override fun createCenterPanel(): JComponent {
                return diffViewer.component
            }
        }
        return if (dialog.showAndGet()) content2.document.text else null
    }

    override fun confirmLibraryInstallation(diffContent: String): String? {
        println("We did not find the correct libraries installed in your project. We will install these now, do you accept?")
        return showDiff("pubspec.yaml changes", "dummy before content", diffContent)
    }

    override fun confirmL10nConfiguration(config: String): Boolean {
        println("We did not find a l10n.yaml file. We will create one with the following configuration, is this okay?")
        println(config)
        println("User clicks 'OK'")
        return true
    }

    override fun confirmProjectFileModification(diffContent: String): String? {
        println("We need to modify your main application file to enable localization. Do you accept the following changes?")
        return showDiff("Application file changes", "dummy before content", diffContent)
    }

    override fun selectAppFile(files: List<String>): String? {
        if (files.isEmpty()) return null
        if (files.size == 1) return files.first()

        println("We found multiple MaterialApp/CupertinoApp declarations. Please choose the correct file:")
        files.forEachIndexed { index, file ->
            println("[$index]: $file")
        }
        // Simulate user choosing the first one
        val choice = 0
        println("User chose index: $choice")
        return files.getOrNull(choice)
    }

    override fun showInfo(title: String, message: String) {
        Messages.showInfoMessage(project, message, title)
    }
}
