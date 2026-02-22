package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.infrastructure.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.ReviewedStringEntry
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service.ReviewedStringsRepository

/**
 * Persists reviewed (skipped) strings using IntelliJ's PropertiesComponent
 * at the project level, so each project has its own set of reviewed strings.
 */
class IdeaReviewedStringsRepository(
    private val project: Project,
    private val objectMapper: ObjectMapper,
) : ReviewedStringsRepository {

    companion object {
        const val REVIEWED_STRINGS_KEY = "GPT_HELPER_REVIEWED_STRINGS_V1"
    }

    private var cache: MutableSet<ReviewedStringEntry>? = null

    override fun getReviewedStrings(): Set<ReviewedStringEntry> {
        val cached = cache
        if (cached != null) {
            return cached.toSet()
        }

        val json = PropertiesComponent.getInstance(project).getValue(REVIEWED_STRINGS_KEY, "")
        if (json.isBlank()) {
            cache = mutableSetOf()
            return emptySet()
        }

        return try {
            val entries: Set<ReviewedStringEntry> = objectMapper.readValue(json)
            cache = entries.toMutableSet()
            entries
        } catch (e: Exception) {
            println("Error loading reviewed strings: ${e.message}")
            cache = mutableSetOf()
            emptySet()
        }
    }

    override fun addReviewedStrings(entries: Set<ReviewedStringEntry>) {
        val current = getReviewedStrings().toMutableSet()
        // Remove old entries with same ID (update them)
        val newIds = entries.map { it.id }.toSet()
        current.removeAll { it.id in newIds }
        current.addAll(entries)
        cache = current
        persist(current)
    }

    override fun removeReviewedStrings(ids: Set<String>) {
        val current = getReviewedStrings().toMutableSet()
        current.removeAll { it.id in ids }
        cache = current
        persist(current)
    }

    override fun clearAll() {
        cache = mutableSetOf()
        persist(emptySet())
    }

    private fun persist(entries: Set<ReviewedStringEntry>) {
        val json = objectMapper.writeValueAsString(entries)
        PropertiesComponent.getInstance(project).setValue(REVIEWED_STRINGS_KEY, json)
    }
}

