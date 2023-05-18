package de.keeyzar.gpthelper.gpthelper.features.review.domain.service

import java.net.URI

fun interface OpenPageService {
    fun openPage(link: URI)
}
