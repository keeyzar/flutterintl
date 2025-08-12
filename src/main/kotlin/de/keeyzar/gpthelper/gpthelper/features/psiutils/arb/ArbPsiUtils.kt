package de.keeyzar.gpthelper.gpthelper.features.psiutils.arb

import com.intellij.json.psi.JsonPsiUtil
import com.intellij.json.psi.impl.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.descendantsOfType
import com.intellij.psi.util.parentOfTypes
import com.intellij.psi.util.siblings

class ArbPsiUtils {

    /**
     * tries to collect all properties from the root object, if they are arrays of strings, e.g.
     *   {
     *     "key": ["value1", "value2"]
     *     "notKey": "value"
     *     "key2": ["value3", "value4"]
     *   }
     *
     * will return:
     *   ["key": ["value1", "value2"], "key2": ["value3", "value4"]]
     */
    fun getAllStringArraysFromRoot(element: PsiElement): List<StringArrayContent> {
        val rootProperties = collectRootProperties(element)
        return rootProperties.map { getStringArrayContent(it) }
    }


    /**
     * collect all json properties on root
     */
    private fun collectRootProperties(element: PsiElement): List<JsonPropertyImpl> {
        return ApplicationManager.getApplication().runReadAction<List<JsonPropertyImpl>> {
            val containingFile = element.containingFile
            if (containingFile !is JsonFileImpl) {
                throw IllegalArgumentException("Provided element is not a json file")
            }
            containingFile.childrenOfType<JsonObjectImpl>()
                .firstOrNull() //root object
                ?.childrenOfType<JsonPropertyImpl>() ?: emptyList()
        }
    }

    /**
     * searches for key, value, desc on root object, e.g.
     * {
     *   "key": "value",
     *   "@key": {
     *     "description": "desc"
     *   },
     *   "nested": {
     *     "nestedKey": "value"
     *   }
     * }
     *
     * will find key, value, desc but not nestedKey or its value
     */
    fun getArbEntryFromKeyOnRootObject(element: PsiElement, key: String): ArbEntry? {
        val rootObject = getRootObject(element)
        if (rootObject != null) {
            val property: JsonPropertyImpl? = rootObject.children.filterIsInstance<JsonPropertyImpl>().find { it.name == key }
            if (property != null) {
                val description = getDescription(property)
                return ArbEntry(property.name, JsonPsiUtil.stripQuotes(property.value?.text ?: ""), description ?: "")
            }
        }
        return null
    }

    /**
     * The goal is to get a json property of the root json object, no matter were the cursor is
     * 1. is this a json property AND is there no parent of json property? -> we have it
     * 2. get the root parent of type JsonProperty -> we have it
     * 2.1. it was null? It might be a whitespace -> is the parent JsonObject and Parent-Parent is a JsonFile? -> get line of cursor, jump to center of line and get the json property
     * 2.2. if not, then we cannot find the json property
     */
    fun getCurrentJsonProperty(element: PsiElement): ArbEntry? {
        if (element is JsonPropertyImpl) {
            val isRootObjectProperty = element.parentOfTypes<JsonPropertyImpl>() == null
            if (isRootObjectProperty) {
                return if (element.name.startsWith("@")) {
                    val description: String? = getDescription(element)
                    val arbEntry = findEntryFromDescription(element)
                    arbEntry?.description = description ?: ""
                    arbEntry
                } else {
                    val description = findCorrespondingDescription(element)
                    ArbEntry(element.name, JsonPsiUtil.stripQuotes(element.value?.text ?: ""), description ?: "")
                }
            }
        }

        //go through the parents, until the parent is psiFile, the last property parent is the one we're looking for
        var currentElem = element.parent
        var lastPropertyParent: JsonPropertyImpl? = null
        while (currentElem != null) {
            if (currentElem is JsonPropertyImpl) {
                lastPropertyParent = currentElem
            }
            if (currentElem is JsonFileImpl) {
                break
            }
            currentElem = currentElem.parent
        }
        if (lastPropertyParent == null) {
            val cursorPosition = element.textOffset
            val line = element.containingFile.viewProvider.document?.getLineNumber(cursorPosition)
            val lineLength = element.containingFile.viewProvider.document?.getLineEndOffset(line!!)
            val lineStart = element.containingFile.viewProvider.document?.getLineStartOffset(line!!)
            val lineCenter = lineStart!! + (lineLength!! - lineStart) / 2
            val elementAtLineCenter: PsiElement? = element.containingFile.findElementAt(lineCenter)
            return if (elementAtLineCenter != null) {
                getCurrentJsonProperty(elementAtLineCenter)
            } else {
                null
            }
        } else {
            return if (lastPropertyParent.name.startsWith("@")) {
                val description = getDescription(lastPropertyParent)
                val entry = findEntryFromDescription(lastPropertyParent)
                entry?.description = description ?: ""
                entry
            } else {
                val description = findCorrespondingDescription(lastPropertyParent)
                ArbEntry(lastPropertyParent.name, JsonPsiUtil.stripQuotes(lastPropertyParent.value?.text ?: ""), description ?: "")
            }
        }
    }

    /**
     * will return the description of the property, if it is a property with a description (starts with @)
     * on root
     */
    private fun getDescription(element: JsonPropertyImpl): String? {
        val isCorrectElement = element.name.startsWith("@")
        if (!isCorrectElement) {
            println("Element should begin with @")
            return null
        }
        return element.descendantsOfType<JsonPropertyImpl>().find { it.name == "description" }?.let {
            return JsonPsiUtil.stripQuotes(it.value?.text ?: "")
        }
    }

    /**
     * will find a description for an element. e.g. will find $@key.description for $key
     */
    private fun findCorrespondingDescription(element: JsonPropertyImpl): String? {
        val nameWithPrefix = "@${element.name}"
        val maybeElem: PsiElement? = element.siblings(forward = true).find {
            it is JsonPropertyImpl && it.name == nameWithPrefix
        }
        return maybeElem?.let {
            return getDescription(it as JsonPropertyImpl)
        }
    }

    /**
     * Will find an arb entry based on a description element, e.g. find $key and the value for $@key
     * most probably, the $key is before $@key - when this is the case, this method is fast. If not, it might be really
     * slow, at worst n-1 iterations (when it's immediately behind it).
     */
    private fun findEntryFromDescription(element: JsonPropertyImpl): ArbEntry? {
        //we have the wrong element, we want to find the property
        //in general it should be before the sibling, but it's not required - it should be fairly fast to find the correct one
        //in most cases
        val nameWithoutPrefix = element.name.removePrefix("@")
        val maybeElem: PsiElement? = element.siblings(forward = false).find {
            it is JsonPropertyImpl && it.name == nameWithoutPrefix
        }

        return if (maybeElem is JsonPropertyImpl) {
            ArbEntry(maybeElem.name, JsonPsiUtil.stripQuotes(maybeElem.value?.text ?: ""))
        } else {
            null
        }
    }

    /**
     * the first child of a valid arb entry is the root object. The file might be empty, though
     */
    private fun getRootObject(element: PsiElement): JsonObjectImpl? {
        return if (element.containingFile is JsonFileImpl) {
            ApplicationManager.getApplication().runReadAction<JsonObjectImpl?> {
                element.children.firstOrNull() as? JsonObjectImpl
            }
        } else {
            null
        }
    }



    /**
     * check if it is an array, if so, check if the items are strings, return all information
     */
    private fun getStringArrayContent(element: JsonPropertyImpl): StringArrayContent {
        val value = element.value
        if (value is JsonArrayImpl) {
            val values = value.children.filterIsInstance<JsonStringLiteralImpl>().map { JsonPsiUtil.stripQuotes(it.text) }
            return StringArrayContent(element.name, values)
        }
        return StringArrayContent(element.name, emptyList())
    }
}