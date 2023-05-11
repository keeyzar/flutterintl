package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.jetbrains.lang.dart.util.DartElementGenerator
import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model.UserSettings
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository

class StatementFixer(private val userSettingsRepository: UserSettingsRepository) {
    /**
     * create a statement like this
     * S.of(context).desiredKey
     * or, if nullable
     * AppLocalizations.of(context)?.desiredKey
     */
    fun fixStatement(project: Project, element: PsiElement, desiredKey: String) {
        val userSettings = userSettingsRepository.getSettings()
        WriteCommandAction.runWriteCommandAction(project) {
            val newStatement = createStatement(desiredKey, userSettings)
            replaceStatementWithNewStatement(element, newStatement)
        }
    }

    private fun replaceStatementWithNewStatement(element: PsiElement, newStatement: String) {
        val keyAccess = DartElementGenerator.createStatementFromText(element.project, newStatement)
        element.parent.replace(keyAccess!!);
    }

    private fun createStatement(desiredKey: String, userSettings: UserSettings): String {
        val nullableGetter = when (userSettings.nullableGetter) {
            true -> "?"
            false -> ""
        }
        val outputClass = userSettings.outputClass

        return "$outputClass.of(context)$nullableGetter.$desiredKey,"
    }


}
