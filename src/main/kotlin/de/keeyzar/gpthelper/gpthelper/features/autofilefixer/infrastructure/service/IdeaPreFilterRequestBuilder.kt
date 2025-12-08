package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterLiteral
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterRequest
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.PreFilterRequestBuilder
import de.keeyzar.gpthelper.gpthelper.features.psiutils.LiteralInContextFinder
import java.util.concurrent.atomic.AtomicInteger

class IdeaPreFilterRequestBuilder(
    private val literalInContextFinder: LiteralInContextFinder
) : PreFilterRequestBuilder {

    companion object {
        private val idCounter = AtomicInteger(0)
    }

    override fun buildRequest(elements: Map<PsiElement, Boolean>): PreFilterRequest {
        // Reset counter for each request to keep IDs simple
        idCounter.set(0)

        val literals = elements.keys.map { psiElement ->
            val id = idCounter.incrementAndGet().toString()
            val literalText = ReadAction.compute<String, Throwable> {
                psiElement.text.removeSurrounding("\"").removeSurrounding("'")
            }
            val contextPsi = ReadAction.compute<PsiElement, Throwable> {
                literalInContextFinder.findContext(psiElement)
            }
            val context = ReadAction.compute<String, Throwable> {
                contextPsi.text
            }

            PreFilterLiteral(
                id = id,
                literalText = literalText,
                context = context,
                psiElement = psiElement
            )
        }

        return PreFilterRequest(literals)
    }
}

