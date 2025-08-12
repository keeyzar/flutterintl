package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.client

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.FileBestGuessContext

data class BestGuessRequest(
    val context: FileBestGuessContext,
) {
}
