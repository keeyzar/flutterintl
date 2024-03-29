package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.controller

import de.keeyzar.gpthelper.gpthelper.features.flutter_intl.infrastructure.service.ArbFilesService
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationFilteredTargetTranslation
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationAndExistingTranslation
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
    /**
     * the user has provided all the relevant information anyway.
     * 1. all the missing entries from untranslated messages. Search for their existing translation in the base language
     * e.g. app_en.arb
     * 2. ask the user, whether he wants to translate all keys, into all languages
     * 3. the selected translations will be translated, currently one after another
     * 4. the user is informed about the progress
     * 5. we trigger translation finished hooks
     */
    suspend fun startMissingTranslationProcess(missingTranslationContext: MissingTranslationContext<T>) {
        val modifiedContext = missingTranslationCollectionService.collectMissingTranslations(missingTranslationContext);
        val baseLanguage = arbFilesService.getBaseLanguage(null)
        val enriched: List<MissingTranslationAndExistingTranslation> = modifiedContext.missingTranslations.map {
            val existingTranslation = existingTranslationRepository.getExistingTranslation(
                missingTranslationContext.reference,
                baseLanguage,
                it.key
            )
            MissingTranslationAndExistingTranslation(it, existingTranslation)
        }
        //alright, we have all the missing and existing translations, now we need to ask the user what he wants to translate
        val enrichedAndUserFiltered: List<MissingTranslationFilteredTargetTranslation> = missingTranslationInputService.collectMissingTranslationInput(enriched)
        modifiedContext.missingTranslationFilteredTargetTranslations = enrichedAndUserFiltered

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
        return missingTranslationContext.missingTranslationFilteredTargetTranslations
            ?.sumOf { it.languagesToTranslateTo.size } ?: 0

    }

    private fun createTranslationRequests(baseLanguage: Language, missingTranslationAndTarget: MissingTranslationFilteredTargetTranslation): UserTranslationRequest {
        val missingTranslation = missingTranslationAndTarget.missingTranslationAndExistingTranslation.missingTranslation
        val existingTranslation = missingTranslationAndTarget
            .missingTranslationAndExistingTranslation
            .existingTranslation
            ?: throw IllegalStateException("Existing translation should not be null")

        val baseTranslation = Translation(
            lang = baseLanguage,
            entry = SimpleTranslationEntry(
                id = "why is there an id",
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
        val alreadyTranslated: MutableList<MissingTranslationFilteredTargetTranslation> = mutableListOf()
        val baseLang = arbFilesService.getBaseLanguage(null)
        missingTranslationContext.missingTranslationFilteredTargetTranslations?.map { missingTranslationTargetTranslation ->
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
        val allTranslations = missingTranslationContext.missingTranslationFilteredTargetTranslations
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