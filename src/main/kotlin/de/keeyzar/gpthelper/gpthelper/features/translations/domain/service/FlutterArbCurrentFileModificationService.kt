package de.keeyzar.gpthelper.gpthelper.features.translations.domain.service

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.CurrentFileModificationContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.CurrentFileModificationException
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.CurrentProjectProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.ImportFixer
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.LastStatementProviderForFlutterArbTranslation
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.StatementFixer

class FlutterArbCurrentFileModificationService(
    private val importFixer: ImportFixer,
    private val statementFixer: StatementFixer,
    private val lastStatementProviderForFlutterArbTranslation: LastStatementProviderForFlutterArbTranslation,
    private val currentProjectProvider: CurrentProjectProvider,
) : CurrentFileModificationService {
    override fun modifyCurrentFile(currentFileModificationContext: CurrentFileModificationContext) {
        val lastStatement = try {
            lastStatementProviderForFlutterArbTranslation.lastStatement!!
        } catch (e: Exception) {
            throw CurrentFileModificationException(
                "Could not get last statement, which should not be possible. The last statement should be the String which you tried to modify",
                e
            )
        }

        try {
            importFixer.addTranslationImportIfMissing(currentProjectProvider.project, lastStatement)
        } catch (e: Exception) {
            throw CurrentFileModificationException(
                "Could not add the import for the file, you should add it manually, if you want to use the translation.",
                e
            )
        }

        try {
            statementFixer.fixStatement(currentProjectProvider.project, lastStatement, currentFileModificationContext.userTranslationInput.desiredKey)
        } catch (e: Exception) {
            throw CurrentFileModificationException(
                "Could not fix the current statement. You should fix it manually.",
                e
            )
        }
    }
}
