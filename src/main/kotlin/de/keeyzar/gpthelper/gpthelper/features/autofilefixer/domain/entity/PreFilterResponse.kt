package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity

/**
 * Response from pre-filtering indicating which literals should be translated
 */
data class PreFilterResponse(
    val results: List<PreFilterResult>
)

data class PreFilterResult(
    val id: String,
    val shouldTranslate: Boolean,
    val reason: String? = null
)

