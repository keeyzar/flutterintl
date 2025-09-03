package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import com.intellij.openapi.application.ApplicationManager
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client.BestGuessResponse
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.MultiKeyTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service.PsiElementIdReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.dto.BestGuessWithPsiReference
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets.BestGuessAdaptionDialog
import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.ProgrammerException
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.SimpleTranslationEntry
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service.FlutterPsiService
import java.util.*

/**
 *
 */
class IdeaBestGuessAdaptionService(
    private val psiElementIdReferenceProvider: PsiElementIdReferenceProvider,
    private val arbFilesService: ArbFilesService,
    private val flutterPsiService: FlutterPsiService,
) : GuessAdaptionService {

    override fun adaptBestGuess(processUUID: UUID, bestGuessResponse: BestGuessResponse): MultiKeyTranslationContext? {

        val bestGuessWithPsiReferenceEntries = bestGuessResponse.responseEntries.map {
            val psiElement = psiElementIdReferenceProvider.getElement(it.id)
                ?: throw ProgrammerException("We have no reference to the element that we want to translate, that means we have made some programming mistake... Sorry, please file an issue!")
            BestGuessWithPsiReference(it.id, psiElement, it)
        }.toList()

        var multiKeyTranslationContext: MultiKeyTranslationContext? = null
        ApplicationManager.getApplication().invokeAndWait {
            val dialog = BestGuessAdaptionDialog(bestGuessWithPsiReferenceEntries, arbFilesService.findAvailableLanguages(), flutterPsiService)
            multiKeyTranslationContext = if (dialog.showAndGet()) {
                val userInput = dialog.getUserInput()
                parseToMultiKeyTranslationContext(processUUID, userInput)
            } else {
                null
            }
        }
        return multiKeyTranslationContext
    }

    private fun parseToMultiKeyTranslationContext(processUUID: UUID, userAdaptedGuesses: BestGuessAdaptionDialog.UserAdaptedGuesses): MultiKeyTranslationContext {
        val baseLanguage = arbFilesService.getBaseLanguage(null)
        return MultiKeyTranslationContext(
            userAdaptedGuesses.adaptedGuesses.map {
                SimpleTranslationEntry(it.id, it.key, it.desiredValue, it.description, it.placeholder)
            }.toList(),
            baseLanguage,
            userAdaptedGuesses.selectedLanguages,
            uuid = processUUID.toString()
        )
    }
}
