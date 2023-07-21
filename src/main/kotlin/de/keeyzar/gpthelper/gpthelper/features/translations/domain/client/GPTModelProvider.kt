package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

abstract class GPTModelProvider {
    abstract fun getAllModels(): List<String>
}
