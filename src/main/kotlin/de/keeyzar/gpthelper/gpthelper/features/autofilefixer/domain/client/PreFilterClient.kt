package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterRequest
import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.PreFilterResponse

/**
 * Client for AI-based pre-filtering of string literals to determine which should be translated
 */
interface PreFilterClient {
    /**
     * Analyzes string literals and determines which ones should be translated
     * @param request The pre-filter request containing literals with context
     * @param progressCallback Optional callback invoked after each batch is processed
     * @return Response indicating which literals should be translated
     */
    suspend fun preFilter(request: PreFilterRequest, progressCallback: ((Int, Int) -> Unit)? = null): PreFilterResponse
}

