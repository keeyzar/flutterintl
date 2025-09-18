package de.keeyzar.gpthelper.gpthelper.features.setup.domain.service

interface YamlModificationService {
    fun addDependency(yamlContent: String, dependencyName: String, dependencyValue: Any): String
    fun addFlutterGenerate(yamlContent: String): String
}
