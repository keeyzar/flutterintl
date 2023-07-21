package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.FileBestGuessContext
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.SingleLiteral
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service.PsiElementIdReferenceProvider
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.presentation.widgets.FoundStringsSelectionDialog
import de.keeyzar.gpthelper.gpthelper.features.psiutils.DartStringLiteralHelper
import de.keeyzar.gpthelper.gpthelper.features.psiutils.PsiElementIdGenerator
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.exception.ProgrammerException
import de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service.ContextProvider
import java.util.*

class IdeaGatherBestGuessContext(
    private val dartStringLiteralHelper: DartStringLiteralHelper,
    private val contextProvider: ContextProvider,
    private val psiElementIdGenerator: PsiElementIdGenerator,
    private val psiElementReferenceContainer: PsiElementIdReferenceProvider
) : GatherBestGuessContext {

    override fun getFileBestGuessContext(processUUID: UUID): FileBestGuessContext? {
        val context = contextProvider.getAutoLocalizeContext(processUUID)
            ?: throw ProgrammerException("The programmer forgot to set the context, or there was some other strange mystery. Anyways, we did not find the context for the process with id: '$processUUID'")
        val stringLiteralsAsPSIElements = dartStringLiteralHelper.findStringPsiElements(context.baseFile)
        var bestGuessContext: FileBestGuessContext? = null
        ApplicationManager.getApplication().invokeAndWait{
//            val bestGuessList = stringLiteralsAsPSIElements.map {
//                BestGuessWithPsiReference(it, BestGuessResponseEntry(
//                    "some id",
//                    "some_key",
//                    "some_descr"
//                ))
//            }
//            BestGuessAdaptionDialog(bestGuessList, listOf(Language("de", null), Language("en", null)))
//                .showAndGet()
            //TODO undo this again, but temporarily show only the nice dialog
            val dialog = FoundStringsSelectionDialog(stringLiteralsAsPSIElements)
            bestGuessContext = if (dialog.showAndGet()) {
                mapToFileBestGuessContext(context.baseFile, dialog.getSelectedElements())
            } else {
                null
            }
        }
        return bestGuessContext
    }

    private fun mapToFileBestGuessContext(psiFile: PsiFile, selectedElements: List<PsiElement>): FileBestGuessContext? {
        return if (selectedElements.isEmpty()) {
            print("The user did not select any psiElement, therefore we return no context")
            null
        } else {
            val stringLiterals = selectedElements
                .fold(ArrayList<SingleLiteral>()) { stringLiterals, psiElement ->
                    val id = psiElementIdGenerator.createIdFromPsiElement(psiElement)
                    psiElementReferenceContainer.putElement(id, psiElement)
                    stringLiterals += SingleLiteral(id, psiElement.text)
                    stringLiterals
                }
            val fileName = psiFile.name
            FileBestGuessContext(fileName, stringLiterals)
        }
    }
}
