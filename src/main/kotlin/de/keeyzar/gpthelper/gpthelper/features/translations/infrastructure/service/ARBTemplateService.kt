package de.keeyzar.gpthelper.gpthelper.features.translations.infrastructure.service

class ARBTemplateService {
    fun fillTemplate(key: String, value: String, description: String): String {
        return template.replace("<key>", key)
            .replace("<value>", value)
            .replace("<description>", description)
    }

    private val template = """
            {
            "<key>": "<value>",
            "@<key>": {
              "description": "<description>"
            }
            }
    """.trimIndent()
}
