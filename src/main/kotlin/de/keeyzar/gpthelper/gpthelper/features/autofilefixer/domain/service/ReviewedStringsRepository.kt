package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.ReviewedStringEntry

/**
 * Repository for persisting strings that were reviewed by the user
 * but not selected for internationalization. This allows the dialog
 * to show these strings in a separate "Previously Skipped" section
 * on subsequent runs, so the user doesn't have to review them again.
 */
interface ReviewedStringsRepository {
    /**
     * Get all previously reviewed (skipped) strings.
     */
    fun getReviewedStrings(): Set<ReviewedStringEntry>

    /**
     * Add entries that the user reviewed but did not select.
     */
    fun addReviewedStrings(entries: Set<ReviewedStringEntry>)

    /**
     * Remove entries (e.g. when the user decides to internationalize
     * a previously skipped string).
     */
    fun removeReviewedStrings(ids: Set<String>)

    /**
     * Clear all reviewed strings (reset).
     */
    fun clearAll()
}

