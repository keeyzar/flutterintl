package de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service.PsiElementIdReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Translation
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.CurrentFileModificationException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.CurrentFileModificationService
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.repository.CurrentProjectProvider
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.LastStatementProviderForFlutterArbTranslation
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.ImportFixer
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.StatementFixer
import java.util.concurrent.Semaphore

class FlutterArbCurrentFileModificationService(
    private val importFixer: ImportFixer,
    private val statementFixer: StatementFixer,
    private val lastStatementProviderForFlutterArbTranslation: LastStatementProviderForFlutterArbTranslation,
    private val currentProjectProvider: CurrentProjectProvider,
    private val psiElementIdReferenceProvider: PsiElementIdReferenceProvider,
) : CurrentFileModificationService {

    companion object {
        private val concurrentFileTranslations = Semaphore(1)
    }

    override fun modifyCurrentFile(translation: Translation) {
        concurrentFileTranslations.acquire()
        try {
            val lastStatement = try {
                val id = translation.entry.id
                if (id == null) {
                    //TODO this is ugly, we should not get the last statement like here... just register it in the psiElementIdReferenceProvider
                    lastStatementProviderForFlutterArbTranslation.lastStatement!!
                } else {
                    psiElementIdReferenceProvider.getElement(id)!!
                }
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
                statementFixer.fixStatement(currentProjectProvider.project, lastStatement, translation.entry.desiredKey)
            } catch (e: Exception) {
                throw CurrentFileModificationException(
                    "Could not fix the current statement. You should fix it manually.",
                    e
                )
            }
        } finally {
            concurrentFileTranslations.release()
        }
    }
}
