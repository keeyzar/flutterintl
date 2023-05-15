package de.keeyzar.gpthelper.gpthelper.features.autofilefixer.domain.service

import java.util.*

interface WaitingIndicatorService {
    fun startWaiting(uuid: UUID, title: String, description: String)
    fun stopWaiting()
}
