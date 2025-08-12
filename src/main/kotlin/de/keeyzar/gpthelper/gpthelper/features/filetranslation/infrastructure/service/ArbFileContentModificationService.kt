package de.keeyzar.gpthelper.gpthelper.features.filetranslation.infrastructure.service

import de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.utils.JsonUtils
import de.keeyzar.gpthelper.gpthelper.features.translations.domain.entity.FileToTranslate

class ArbFileContentModificationService(private val jsonUtils: JsonUtils) {
    fun appendTranslation(fileToTranslate: FileToTranslate, newContent: Map<String, Any>): FileToTranslate {
        val newContentToAppend = jsonUtils.mapToJsonString(newContent)
        val newFileContent = appendNewContent(fileToTranslate.content, newContentToAppend)
        return fileToTranslate.copy(content = newFileContent);
    }

    private fun appendNewContent(fileContent: String, newContent: String): String {
        var newFile = findAndReplaceWithExistingEntry(fileContent, newContent)
        if (newFile == null) {
            newFile = findAndReplaceWithoutEntry(fileContent, newContent)
        }
        return newFile
    }

    private fun findAndReplaceWithoutEntry(fileContent: String, newContent: String): String {
        val lastIndex = fileContent.lastIndexOf("}")
        return if (lastIndex != -1) {
            fileContent.substring(0, lastIndex) + " \n " + newContent + " \n}"
        } else {
            " {\n $newContent \n}"
        }
    }

    private fun findAndReplaceWithExistingEntry(fileContent: String, newContent: String): String? {
        val hasEntries = jsonUtils.hasAnyEntry(fileContent)
        if (!hasEntries) return null;

        val lastIndex = fileContent.lastIndexOf("}")
        if (lastIndex != -1) {
            return fileContent.substring(0, lastIndex) + ",\n " + newContent + "\n}"

        }
        return null
    }

}
