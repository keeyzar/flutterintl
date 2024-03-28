package de.keeyzar.gpthelper.gpthelper.features.missingtranslations.infrastructure.service

import com.intellij.psi.PsiElement
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslation
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.entity.MissingTranslationContext
import de.keeyzar.gpthelper.gpthelper.features.missingtranslations.domain.service.MissingTranslationCollectionService
import de.keeyzar.gpthelper.gpthelper.features.psiutils.arb.ArbPsiUtils
import de.keeyzar.gpthelper.gpthelper.features.psiutils.arb.StringArrayContent
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.Language

class MissingTranslationCollectionServiceIdea(
    private val arbPsiUtils: ArbPsiUtils,
) : MissingTranslationCollectionService<PsiElement> {
    override fun collectMissingTranslations(missingTranslationContext: MissingTranslationContext<PsiElement>): MissingTranslationContext<PsiElement> {
        //essentially, we're collecting de -> "greeting" and pl -> "greeting" and fr -> "greeting"
        //afterward, we invert it to be "greeting" -> de, pl, fr
        val allStringArraysFromRoot: List<StringArrayContent> = arbPsiUtils.getAllStringArraysFromRoot(missingTranslationContext.reference)

        val collectToMap: Map<String, List<String>> = allStringArraysFromRoot
            .flatMap { stringArrayContent -> stringArrayContent.values.map { arbEntryKey -> Pair(arbEntryKey, stringArrayContent.key) } }
            .groupBy({ it.first }, { it.second })

        val missingTranslations: List<MissingTranslation> = collectToMap
            .map { outer -> MissingTranslation(outer.key, outer.value.map { getLanguageFromString(it) }) }

        return MissingTranslationContext(uuid = missingTranslationContext.uuid, missingTranslations, reference = missingTranslationContext.reference)
    }
    private fun getLanguageFromString(language: String): Language {
        //either it's de or de_DE (or en_US or en)
        val split = language.split("_")
        return if (split.size == 2) {
            Language(split[0], split[1])
        } else {
            Language(split[0], null)
        }
    }
}