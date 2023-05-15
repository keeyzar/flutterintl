package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.entity.FileBestGuessContext
import java.util.UUID

/**
 * should get the relevant data from the user
 */
fun interface GatherBestGuessContext {
    /**
     * i.e. ask user or something like that for the information
     * @return null, if the information process was canceled, e.g. by the user
     */
    fun getFileBestGuessContext(processUUID: UUID): FileBestGuessContext?
}
