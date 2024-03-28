package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.exceptions.GatherContextException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.ArbFilenameParser
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserSettingsRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserTranslationInputRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.GatherTranslationContextService
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.LastStatementProviderForFlutterArbTranslation

class FlutterArbTranslationContextService(
    private val lastStatementProviderForFlutterArbTranslation: LastStatementProviderForFlutterArbTranslation,
    private val settingsRepository: UserSettingsRepository,
    private val userTranslationInputRepository: UserTranslationInputRepository,
    private val arbFilenameParser: ArbFilenameParser,
    private val psiElementService: FlutterPsiService,
    private val arbFilesService: ArbFilesService
) : GatherTranslationContextService {
    override fun gatherTranslationContext(statement: String?): TranslateKeyContext {
        val lastStatement = statement ?: getLastStatement()
        val userSettings = try {
            settingsRepository.getSettings();
        } catch (e: Exception) {
            throw GatherContextException("Could not get the user settings, these should've been verified by now", e)
        }
        val lastUserInput = try {
            userTranslationInputRepository.getAllTranslationDialogData()
        } catch (e: Exception) {
            throw GatherContextException("There was an issue getting information from your latest translations", e)
        }
        val baseLanguage = try {
            arbFilenameParser.getLanguageFromFilename(userSettings.templateArbFile)
        } catch (e: Exception) {
            throw GatherContextException(
                "There was an issue with the template arb file (your base language), you might want to ensure the name is something like app_en_US.arb, the current filename is '${userSettings.templateArbFile}'",
                e
            )
        }
        val availableLanguages = arbFilesService.findAvailableLanguages()

        return TranslateKeyContext(
            statement = lastStatement,
            baseLanguage = baseLanguage,
            lastUserInput = lastUserInput,
            availableLanguages = availableLanguages,
        )
    }

    /**
     *
     */
    private fun getLastStatement(): String {
        val lastStatementPsi = lastStatementProviderForFlutterArbTranslation.lastStatement
            ?: throw GatherContextException("There should be a last statement, but there is none, this might be a programming error!")
        return try {
            psiElementService.getStringFromDartLiteral(lastStatementPsi)
        } catch (e: Exception) {
            throw GatherContextException("There was an issue getting the last statement, this might be a programming error!", e)
        }
    }
}
