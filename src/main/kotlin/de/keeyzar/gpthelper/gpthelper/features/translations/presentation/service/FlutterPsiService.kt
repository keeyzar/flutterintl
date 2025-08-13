package de.keeyzar.gpthelper.gpthelper.features.translations.presentation.service

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.elementType
import com.jetbrains.lang.dart.DartTokenTypes
import com.jetbrains.lang.dart.psi.*
import de.keeyzar.gpthelper.gpthelper.features.translations.presentation.exception.PsiElementException

class FlutterPsiService {
    fun getStringFromDartLiteral(psiElement: PsiElement): String {
        return when (psiElement.elementType) {
            DartTokenTypes.REGULAR_STRING_PART -> removeSurroundingQuotesParent(psiElement)
            DartTokenTypes.OPEN_QUOTE -> removeSurroundingQuotesParent(psiElement)
            DartTokenTypes.CLOSING_QUOTE -> removeSurroundingQuotesParent(psiElement)
            DartTokenTypes.STRING_LITERAL_EXPRESSION,
            DartTokenTypes.RAW_TRIPLE_QUOTED_STRING,
            DartTokenTypes.RAW_SINGLE_QUOTED_STRING -> removeSurroundingQuotes(psiElement)
            else -> throw PsiElementException("PsiElement is not a string literal, it's a ${psiElement.elementType}, text: ${psiElement.text}")
        }
    }

    fun getParentFromDartLiteral(psiElement: PsiElement): PsiElement {
        return when (psiElement.elementType) {
            DartTokenTypes.REGULAR_STRING_PART -> psiElement.parent
            DartTokenTypes.OPEN_QUOTE -> psiElement.parent
            DartTokenTypes.CLOSING_QUOTE -> psiElement.parent
            DartTokenTypes.STRING_LITERAL_EXPRESSION,
            DartTokenTypes.RAW_TRIPLE_QUOTED_STRING,
            DartTokenTypes.RAW_SINGLE_QUOTED_STRING -> psiElement
            else -> throw PsiElementException("PsiElement is not a string literal, it's a ${psiElement.elementType}, text: ${psiElement.text}")
        }
    }

    private fun removeSurroundingQuotesParent(psiElement: PsiElement) = removeSurroundingQuotes(psiElement.parent)

    private fun removeSurroundingQuotes(psiElement: PsiElement) = psiElement.text
        .removeSurrounding("\"\"\"")
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .removeSurrounding("'''")
        .trim()


    fun isDartString(element: PsiElement): Boolean {
        return element.elementType == DartTokenTypes.REGULAR_STRING_PART
                || element.elementType == DartTokenTypes.OPEN_QUOTE
                || element.elementType == DartTokenTypes.CLOSING_QUOTE
    }



    /**
     * Finds the name of a 'BuildContext' variable available in the scope of the given PSI element.
     *
     * It performs the search in the following order:
     * 1.  Parameters of the containing method or function.
     * 2.  Local variables declared within the containing method or function.
     * 3.  Fields of the containing class.
     * 4.  Fields of any superclasses (walking up the inheritance chain).
     *
     * @param element The PSI element from where the search should start.
     * @return The name of the BuildContext variable (e.g., "context"), or null if none is found.
     */
    fun findBuildContextName(element: PsiElement): String? {
        // 1. Search in the scope of the current function/method
        val containingMethod = PsiTreeUtil.getParentOfType(element, DartComponent::class.java)
        if (containingMethod != null) {
            // Check method parameters
            val params = PsiTreeUtil.findChildrenOfType(containingMethod, DartSimpleFormalParameter::class.java)
            for (param in params) {
                if (param.type?.text == "BuildContext") {
                    return param.componentName.text
                }
            }

            // Check local variables
            val localVars = PsiTreeUtil.findChildrenOfType(containingMethod, DartVarDeclarationList::class.java)
            for (localVar in localVars) {
                val variableType = PsiTreeUtil.findChildOfType(localVar, DartType::class.java)
                if (variableType?.text == "BuildContext") {
                    return (variableType.parent as DartVarAccessDeclaration).nameIdentifier?.text
                }
            }
        }

        // 2. Search in the containing class and its superclasses
        var currentClass = PsiTreeUtil.getParentOfType(element, DartClassDefinition::class.java)
        while (currentClass != null) {
            val fields = currentClass.classBody?.classMembers?.varDeclarationListList ?: emptyList()
            for (field in fields) {
                val fieldType = PsiTreeUtil.findChildOfType(field, DartType::class.java)
                if (fieldType?.text == "BuildContext") {
                    return (fieldType.parent as DartVarAccessDeclaration).nameIdentifier?.text
                }
            }
            // Move up to the superclass
            currentClass = currentClass.superClass?.referenceExpression?.resolve() as? DartClassDefinition
        }

        return null
    }
}
