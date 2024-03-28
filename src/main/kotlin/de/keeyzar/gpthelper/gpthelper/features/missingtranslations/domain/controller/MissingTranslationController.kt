package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.controller

import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationTargetTranslation
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationWithExistingTranslation
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.repository.ExistingTranslationRepository
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.service.MissingTranslationCollectionService
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.service.MissingTranslationInputService
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.ThreadingService
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.*
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.OngoingTranslationHandler
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.TranslationErrorProcessHandler
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.TranslationProgressBus
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.TranslationTriggeredHooks

class MissingTranslationController<T>(
    private val missingTranslationCollectionService: MissingTranslationCollectionService<T>,
    private val arbFilesService: ArbFilesService,
    private val existingTranslationRepository: ExistingTranslationRepository<T>,
    private val missingTranslationInputService: MissingTranslationInputService,
    private val threadingService: ThreadingService<MissingTranslationContext<T>>,
    private val ongoingTranslationHandler: OngoingTranslationHandler,
    private val translationErrorProcessHandler: TranslationErrorProcessHandler,
    private val translationProgressBus: TranslationProgressBus,
    private val translationTriggeredHooks: TranslationTriggeredHooks,
) {
    suspend fun startMissingTranslationProcess(missingTranslationContext: MissingTranslationContext<T>) {
        //2. collect all translate key contexts of the keys from the base language arb file,
        //e.g. "de" is missing key "hello" -> collect from base language en => value + description
        //3. show user ui (left a list with missing translations, right a view of the translation and their languages
        //4. user checks all translations and marks them as "yes, please translate
        //5. run the translation for one missing key after another and show in progress, accordingly
        //do not allow to change key or value or description
        val modifiedContext = missingTranslationCollectionService.collectMissingTranslations(missingTranslationContext);
        val baseLanguage = arbFilesService.getBaseLanguage(null)
        val enriched: List<MissingTranslationWithExistingTranslation> = modifiedContext.missingTranslations.map {
            val existingTranslation = existingTranslationRepository.getExistingTranslation(
                missingTranslationContext.reference,
                baseLanguage,
                it.key
            )
            MissingTranslationWithExistingTranslation(it, existingTranslation)
        }
        //alright, we have all the missing and existing translations, now we need to ask the user what he wants to translate
        val enrichedAndFiltered: List<MissingTranslationTargetTranslation> = missingTranslationInputService.collectMissingTranslationInput(enriched)
        modifiedContext.missingTranslationTargetTranslations = enrichedAndFiltered
        val taskAmount = calculateTaskAmount(modifiedContext)
        modifiedContext.taskAmount = taskAmount
        //user is happy, we can now translate all keys for the languages accordingly
        threadingService.putIntoQueue(modifiedContext)
        threadingService.startQueueIfNotRunning {
            try {
                translationTaskHandler(it)
            } catch (e: Throwable) {
                it.finished = true
                reportProgress(it)
                translationErrorProcessHandler.displayErrorToUser(e)
            }
        }
    }

    private fun calculateTaskAmount(missingTranslationContext: MissingTranslationContext<T>): Int {
        return missingTranslationContext.missingTranslationTargetTranslations
            ?.sumOf { it.languagesToTranslateTo.size } ?: 0

    }

    private fun createTranslationRequests(baseLanguage: Language, missingTranslationAndTarget: MissingTranslationTargetTranslation): UserTranslationRequest {
        val missingTranslation = missingTranslationAndTarget.missingTranslationWithExistingTranslation.missingTranslation
        val existingTranslation = missingTranslationAndTarget
            .missingTranslationWithExistingTranslation
            .existingTranslation
            ?: throw IllegalStateException("Existing translation should not be null")

        val baseTranslation = Translation(
            lang = baseLanguage,
            entry = SimpleTranslationEntry(
                id = "TODO which id",
                desiredKey = missingTranslation.key,
                desiredValue = existingTranslation.value,
                desiredDescription = existingTranslation.description ?: ""
            )
        )
        return UserTranslationRequest(
            targetLanguages = missingTranslationAndTarget.languagesToTranslateTo,
            baseTranslation = baseTranslation
        )
    }

    private suspend fun translationTaskHandler(missingTranslationContext: MissingTranslationContext<T>) {
        val alreadyTranslated: MutableList<MissingTranslationTargetTranslation> = mutableListOf()
        val baseLang = arbFilesService.getBaseLanguage(null)
        missingTranslationContext.missingTranslationTargetTranslations?.map { missingTranslationTargetTranslation ->
            createTranslationRequests(baseLang, missingTranslationTargetTranslation)
                .let { req ->
                    if (Thread.interrupted() || missingTranslationContext.isCancelled()) {
                        return
                    }
                    try {
                        ongoingTranslationHandler.translateAsynchronously(req) {
                            reportProgress(missingTranslationContext)
                            alreadyTranslated += missingTranslationTargetTranslation
                        }
                    } catch (e: Throwable) {
                        missingTranslationContext.finishedTasks++
                        reportProgress(missingTranslationContext)
                    }
                }
        }
        val allTranslations = missingTranslationContext.missingTranslationTargetTranslations
        val translationsWithIssues = allTranslations?.filter { !alreadyTranslated.contains(it) }
        missingTranslationContext.translationsWithIssues = translationsWithIssues
    }

    private fun reportProgress(missingTranslationContext: MissingTranslationContext<T>) {
        val taskAmount = missingTranslationContext.taskAmount
        val taskAmountHandled = ++missingTranslationContext.finishedTasks
        val id = missingTranslationContext.uuid
        missingTranslationContext.progressText = "Missing translations: $taskAmountHandled/$taskAmount"
        val translationProgress = TranslationProgress(taskAmount, taskAmountHandled, id)

        if (taskAmountHandled + 1 == taskAmount) {
            missingTranslationContext.finished = true
            translationTriggeredHooks.translationTriggeredPostTranslation()
        }
        translationProgressBus.pushPercentage(translationProgress)
    }

}