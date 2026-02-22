package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity

/**
 * Represents a string literal that was reviewed by the user and explicitly
 * not selected for internationalization.
 *
 * @param id A stable hash based on relativeFilePath + literalText
 * @param relativeFilePath The file path relative to the project root
 * @param literalText The text content of the string literal (without quotes)
 * @param reviewedAt Timestamp when the user reviewed this string
 */
data class ReviewedStringEntry(
    val id: String,
    val relativeFilePath: String,
    val literalText: String,
    val reviewedAt: Long,
)

