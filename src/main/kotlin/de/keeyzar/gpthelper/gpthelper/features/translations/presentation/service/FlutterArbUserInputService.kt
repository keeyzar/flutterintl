package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.openapi.application.ApplicationManager
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.TranslateKeyContext
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.UserTranslationInput
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.repository.UserTranslationInputRepository
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.service.GatherUserInputService
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.pojo.TranslationDialogUserInput
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.widgets.GenerateTranslationDialog
import java.util.concurrent.atomic.AtomicReference

class FlutterArbUserInputService(
    private val userTranslationInputRepository: UserTranslationInputRepository,
    ) : GatherUserInputService {
    override fun requestInformationFromUser(translateKeyContext: TranslateKeyContext): UserTranslationInput? {
        val dialogInput = AtomicReference<TranslationDialogUserInput>()
        ApplicationManager.getApplication().invokeAndWait {
            val generateTranslationDialog = GenerateTranslationDialog(translateKeyContext)
            val closedWithOk = generateTranslationDialog.showAndGet()
            if (closedWithOk) {
                val newDialogInput: TranslationDialogUserInput = generateTranslationDialog.getDialogInput()
                dialogInput.set(newDialogInput)
                userTranslationInputRepository.appendTranslationDialogData(UserTranslationInput(
                    newDialogInput.desiredKey,
                    newDialogInput.desiredValue,
                    newDialogInput.desiredDescription,
                    newDialogInput.translationsChecked
                ));
            }
        }
        val dialogUserInput = dialogInput.get()
        return if (dialogUserInput != null) {
            mapToUserTranslationInput(dialogUserInput)
        } else {
            null
        }
    }

    private fun mapToUserTranslationInput(dialogInput: TranslationDialogUserInput): UserTranslationInput {
        //might be, that we need to validate some more stuff here, but for now, this is fine.
        return UserTranslationInput(
            desiredKey = dialogInput.desiredKey,
            desiredValue = dialogInput.desiredValue,
            desiredDescription = dialogInput.desiredDescription,
            languagesToTranslate = dialogInput.translationsChecked
        )
    }

}
