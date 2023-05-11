package de.keeyzar.gpthelper.gpthelper.features.filetranslation.domain.entity

/**
 * Context for the translation in general,
 * while metadata is some extra data you can use to push some additional data through the domain
 */
interface Context<T> {
    fun getMetadata(): T
}
