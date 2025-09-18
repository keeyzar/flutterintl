package de.keeyzar.gpthelper.gpthelper.features.setup.presentation.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.shared.presentation.actions.ProjectAwareAction
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.dependencyinjection.FlutterArbTranslationInitializer

/**
 * When a project is started / we check after some seconds (e.g. 5) whether the project is set up for intl
 * if not, we show a notification: Do you want to set up intl for your project?
 * if he clicks it, we start the intl setup
 * - the user can also trigger that via right clicking the project and choosing "set-up intl"
 *
 * the process is as follows
 * 1. check if the user has the correct libs installed, if not, ask if we should install the libs
 * 2. check if the user has in root a l10n.yaml configuration, if not, ask if we should create it with default
 * parameters
 * 3. get the settings and check if the main language (e.g. lib/l10n/en_EN.arb) exists, if not, create file in dir
 * 4. search in the whole project for runApp function method call: ask the user: is this your main file
 * if the user says yes - we send the file to gemini and ask: how can we integrate l10n here:
 * we need delegates, etc.
 * 5. show the user: hey, this is the difference (a diff window possibly exists already), do you want to accept?
 * 6. that's it, intl is set up
 *
 * in each step, ask the user, whether he's fine with the changes. We may even add the possibility to use
 * Localizations without any context at all - and when we check for buildContext variable, and we find none, we just
 * use the no context l10n
 */
class SetupIntlAction : ProjectAwareAction() {
    override fun actionPerformed(e: AnActionEvent, project: Project, initializer: FlutterArbTranslationInitializer) {
        initializer.setupService.orchestrate()
    }
}