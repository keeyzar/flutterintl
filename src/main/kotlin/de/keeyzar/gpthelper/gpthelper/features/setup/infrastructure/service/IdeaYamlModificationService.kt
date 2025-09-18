package de.keeyzar.gpthelper.gpthelper.features.setup.infrastructure.service

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.util.PsiTreeUtil
import de.keeyzar.gpthelper.gpthelper.features.setup.domain.service.YamlModificationService
import org.jetbrains.yaml.YAMLElementGenerator
import org.jetbrains.yaml.YAMLFileType
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

class IdeaYamlModificationService(private val project: Project) : YamlModificationService {

    override fun addDependency(yamlContent: String, dependencyName: String, dependencyValue: Any): String {
        val yamlFile = createYamlFile(yamlContent)
        WriteCommandAction.runWriteCommandAction(project) {
            val dependenciesMapping = findOrCreateMapping(yamlFile, "dependencies")
            val generator = YAMLElementGenerator.getInstance(project)
            val newKeyValue = when (dependencyValue) {
                is String -> generator.createYamlKeyValue(dependencyName, dependencyValue)
                is Map<*, *> -> {
                    // Note: The value for a map must start with a newline to be parsed correctly as a block.
                    val valueText = (dependencyValue as Map<String, String>).entries.joinToString("\n") {
                        "  %s: %s".format(it.key, it.value) // Ensure children are indented
                    }
                    generator.createYamlKeyValue(dependencyName, "\n" + valueText)
                }
                else -> throw IllegalArgumentException("Unsupported dependency value type")
            }

            // Use putKeyValue instead of add to handle formatting correctly.
            // It will replace an existing key or add a new one with proper indentation.
            dependenciesMapping.putKeyValue(newKeyValue)
        }
        return yamlFile.text
    }

    override fun addFlutterGenerate(yamlContent: String): String {
        val yamlFile = createYamlFile(yamlContent)
        WriteCommandAction.runWriteCommandAction(project) {
            val flutterMapping = findOrCreateMapping(yamlFile, "flutter")
            val generator = YAMLElementGenerator.getInstance(project)
            // The value 'true' needs to be treated as a boolean, not a string, in YAML.
            // However, the generator often works best with string representations.
            // For simple values, this is fine.
            val newKeyValue = generator.createYamlKeyValue("generate", "true")

            // Use putKeyValue to ensure correct formatting (newline and indentation).
            flutterMapping.putKeyValue(newKeyValue)
        }
        return yamlFile.text
    }

    private fun createYamlFile(content: String): YAMLFile {
        return PsiFileFactory.getInstance(project)
            .createFileFromText("dummy.yaml", YAMLFileType.YML, content) as YAMLFile
    }

    /**
     * Finds a YAML mapping for the given key. If the key does not exist or its value is not a mapping,
     * it creates and adds a new, empty mapping.
     */
    private fun findOrCreateMapping(yamlFile: YAMLFile, key: String): YAMLMapping {
        val rootMapping = PsiTreeUtil.findChildOfType(yamlFile, YAMLMapping::class.java)
            ?: throw IllegalStateException("Could not find root mapping in YAML file.")

        val targetKeyValue = rootMapping.getKeyValueByKey(key)

        // Case 1: The mapping already exists. Return it.
        if (targetKeyValue?.value is YAMLMapping) {
            return targetKeyValue.value as YAMLMapping
        }

        val generator = YAMLElementGenerator.getInstance(project)
        // Create a new key-value with a dummy entry. This ensures the value is parsed as a YAMLMapping.
        val newKeyValueWithDummy = generator.createYamlKeyValue(key, "\n  dummy: temp")

        val attachedKeyValue: YAMLKeyValue
        if (targetKeyValue != null) {
            // Case 2: The key exists, but the value is not a mapping (e.g., `dependencies:`). Replace it.
            attachedKeyValue = targetKeyValue.replace(newKeyValueWithDummy) as YAMLKeyValue
        } else {
            // Case 3: The key does not exist. Add it to the root mapping.
            rootMapping.add(generator.createEol())
            attachedKeyValue = rootMapping.add(newKeyValueWithDummy) as YAMLKeyValue
        }

        // Now that the new element is attached to the main PSI tree, we can safely modify it.
        val newMapping = attachedKeyValue.value as? YAMLMapping
            ?: throw IllegalStateException("Could not create mapping for key '$key'.")

        // Find and delete the dummy entry to leave an empty, ready-to-use mapping.
        newMapping.getKeyValueByKey("dummy")?.delete()

        return newMapping
    }
}