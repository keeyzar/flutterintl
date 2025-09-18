package de.keeyzar.gpthelper.gpthelper.features.setup.domain.model

/**
 * A simple domain model to pass data between the steps of the setup process.
 */
data class SetupProcessModel(
    var pubspecContent: String,
    var l10nFileContent: String? = null,
    var mainAppFileContent: String,
    var mainAppFilePath: String = "lib/main.dart" // Default path, can be updated
)

