package de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils

import com.fasterxml.jackson.core.util.DefaultIndenter
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule

class ObjectMapperProvider() {
    fun provideObjectMapper(yamlFactory: YAMLFactory?): ObjectMapper {
        val prettyPrinter = DefaultPrettyPrinter().withObjectIndenter(DefaultIndenter().withLinefeed("\n"))

        var objectMapper = when(yamlFactory) {
            null -> ObjectMapper()
            else -> ObjectMapper(yamlFactory)
        }
        objectMapper = objectMapper.registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )
        objectMapper.setDefaultPrettyPrinter(prettyPrinter)
        return objectMapper
    }
}

