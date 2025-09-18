package de.keeyzar.gpthelper.gpthelper.features.setup.domain.service

import de.keeyzar.gpthelper.gpthelper.features.setup.domain.model.SetupProcessModel
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.PathProvider
import de.keeyzar.gpthelper.gpthelper.features.shared.domain.service.InstallFileProvider
import org.yaml.snakeyaml.Yaml
import java.nio.file.Paths

class SetupService(
    private val pathProvider: PathProvider,
    private val userInstallDialogs: UserInstallDialogs,
    private val fileProvider: InstallFileProvider,
    private val appReferenceProvider: AppReferenceProvider,
    private val yamlModificationService: YamlModificationService
) {
    /**
     * Orchestrates the entire setup process, step by step.
     */
    fun orchestrate() {
        println("Starting setup orchestration...")
        val rootPath = pathProvider.getRootPath()
        val pubspecPath = Paths.get(rootPath, "pubspec.yaml").toString()
        val l10nPath = Paths.get(rootPath, "l10n.yaml").toString()
        val mainAppPath = Paths.get(rootPath, "lib", "main.dart").toString()

        val pubspecContent = fileProvider.readFile(pubspecPath) ?: ""
        val l10nFileContent = fileProvider.readFile(l10nPath)
        val mainAppFileContent = fileProvider.readFile(mainAppPath) ?: ""

        if (pubspecContent.isEmpty()) {
            //show dialog - we did not find a pubspec file
            return
        }

        val model = SetupProcessModel(
            pubspecContent = pubspecContent,
            l10nFileContent = l10nFileContent,
            mainAppFileContent = mainAppFileContent,
            mainAppFilePath = mainAppPath
        )

        // Step 1: Check and install dependencies
        if (!isInstalled(model)) {
            val success = installIntl(model, pubspecPath)
            if (!success) {
                println("User declined dependency installation. Aborting.")
                return
            }
        } else {
            println("Dependencies are already installed.")
        }

        // Step 2: Check and configure l10n.yaml
        if (!isL10nConfigured(model, l10nPath)) {
            val success = configureIntl(model, l10nPath)
            if (!success) {
                println("User declined l10n.yaml configuration. Aborting.")
                return
            }
        } else {
            println("l10n.yaml is already configured.")
        }

        // Step 3: Modify MaterialApp/CupertinoApp
        val references = appReferenceProvider.findAppReferences()
        if (!isProjectCombined(references)) {
            val success = combineIntlAndProject(references)
            if (!success) {
                println("User declined project file modification. Aborting.")
                return
            }
        } else {
            println("Project already seems to be configured for localization.")
        }

        println("Setup orchestration finished successfully!")
    }

    /**
     * Checks if flutter_localizations and intl are in pubspec.yaml.
     * Uses real YAML parsing.
     */
    fun isInstalled(model: SetupProcessModel): Boolean {
        val yaml = Yaml()
        val pubspec = yaml.load<Map<String, Any>>(model.pubspecContent)
        return (pubspec["dependencies"] as? Map<String, *>)?.run {
            containsKey("flutter_localizations") && containsKey("intl")
        } ?: false
    }

    /**
     * Adds intl and generate:true to pubspec.yaml after user confirmation.
     * Uses real YAML parsing and writing.
     */
    fun installIntl(model: SetupProcessModel, pubspecPath: String): Boolean {
        var newContent = model.pubspecContent

        newContent = yamlModificationService.addDependency(newContent, "flutter_localizations", mapOf("sdk" to "flutter"))
        newContent = yamlModificationService.addDependency(newContent, "intl", "any")
        newContent = yamlModificationService.addFlutterGenerate(newContent)

        val acceptedContent = userInstallDialogs.showDiff("Install Localization Libraries", model.pubspecContent, newContent)
        if (acceptedContent != null) {
            model.pubspecContent = acceptedContent
            fileProvider.writeFile(pubspecPath, acceptedContent)
            return true
        }
        return false
    }

    /**
     * Checks if l10n.yaml exists and contains required keys.
     */
    fun isL10nConfigured(model: SetupProcessModel, l10nPath: String): Boolean {
        if (!fileProvider.fileExists(l10nPath)) return false
        val yaml = Yaml()
        return yaml.load<Map<String, Any>>(fileProvider.readFile(l10nPath) ?: return false).run {
            listOf("arb-dir", "template-arb-file", "output-localization-file")
                .all { containsKey(it) }
        }
    }

    /**
     * Creates a default l10n.yaml file after user confirmation.
     */
    fun configureIntl(model: SetupProcessModel, l10nPath: String): Boolean {
        val l10nConfigMap = mapOf(
            "arb-dir" to "lib/l10n",
            "template-arb-file" to "app_en.arb",
            "output-localization-file" to "app_localizations.dart",
            "untranslated-messages-file" to "untranslated_messages.txt",
            "nullable-getter" to true
        )
        val yaml = Yaml()
        val l10nConfig = yaml.dump(l10nConfigMap)
        val accepted = userInstallDialogs.confirmL10nConfiguration(l10nConfig)
        if (accepted) {
            model.l10nFileContent = l10nConfig
            fileProvider.writeFile(l10nPath, l10nConfig)
        }
        return accepted
    }

    /**
     * Checks if the project's MaterialApp/CupertinoApp is already configured.
     */
    fun isProjectCombined(references: List<Any>): Boolean {
        if (references.isEmpty()) return false
        return references.any { appReferenceProvider.referenceHasLocalization(it) }
    }

    /**
     * Sucht alle App-Referenzen und modifiziert die gewählte für Lokalisierung.
     */
    fun combineIntlAndProject(references: List<Any>): Boolean {
        if (references.isEmpty()) {
            println("Keine MaterialApp oder CupertinoApp gefunden.")
            return false
        }
        val chosenReference = when {
            references.size == 1 -> references.first()
            else -> {
                val fileList = references.map { it.toString() }
                val chosenPath = userInstallDialogs.selectAppFile(fileList)
                references.find { it.toString() == chosenPath } ?: return false
            }
        }
        val originalContent = appReferenceProvider.getContent(chosenReference) ?: return false
        val dummyContent = appReferenceProvider.enableLocalizationOnDummy(chosenReference) ?: return false
        val acceptedContent = userInstallDialogs.showDiff("Enable Localization in App", originalContent, dummyContent)
        if (acceptedContent != null) {
            appReferenceProvider.modifyFileContent(chosenReference, acceptedContent)
            return true
        } else {
            return false
        }
    }
}

// Dummy main function to demonstrate the flow
fun main() {
//    val pathProvider = IdeaPathProvider()
//    val userDialogs = IdeaUserInstallDialogs()
//    val fileProvider = IdeaInstallFileProvider()
//    val rootPath = pathProvider.getRootPath()
//    val appReferenceProvider = IdeaAppReferenceProvider(rootPath)
//    val setupService = SetupService(pathProvider, userDialogs, fileProvider, appReferenceProvider)
//    setupService.orchestrate()
}
