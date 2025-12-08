package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterLiteral
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterRequest
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.PreFilterRequestBuilder
import de.keeyzar.gpthelper.gpthelper.features.psiutils.LiteralInContextFinder
import de.keeyzar.gpthelper.gpthelper.features.psiutils.SmartPsiElementWrapper
import java.util.concurrent.atomic.AtomicInteger

class IdeaPreFilterRequestBuilder(
    private val literalInContextFinder: LiteralInContextFinder
) : PreFilterRequestBuilder {

    companion object {
        private val idCounter = AtomicInteger(0)
    }

    override fun buildRequest(elements: Map<SmartPsiElementWrapper<PsiElement>, Boolean>): PreFilterRequest {
        // Reset counter for each request to keep IDs simple
        idCounter.set(0)

        // Resolve all pointers and filter out invalid elements
        val literals = elements.mapNotNull { (wrapper, _) ->
            // SmartPointer.getElement() internally calls isValid() which requires read access
            val psiElement = ReadAction.compute<PsiElement?, Throwable> {
                wrapper.element
            }

            psiElement?.let {
                val id = idCounter.incrementAndGet().toString()
                val literalText = ReadAction.compute<String, Throwable> {
                    it.text.removeSurrounding("\"").removeSurrounding("'")
                }
                val contextPsi = ReadAction.compute<PsiElement, Throwable> {
                    literalInContextFinder.findContext(it)
                }
                val context = ReadAction.compute<String, Throwable> {
                    contextPsi.text
                }

                PreFilterLiteral(
                    id = id,
                    literalText = literalText,
                    context = context,
                    psiElement = it
                )
            }
        }

        return PreFilterRequest(literals)
    }
}

