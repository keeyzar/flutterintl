package de.keeyzar.gpthelper.gpthelper.features.psiutils.arb

import com.intellij.json.psi.JsonPsiUtil
import com.intellij.json.psi.impl.*
import com.intellij.psi.PsiElement
import com.intellij.psi.util.childrenOfType
import com.intellij.psi.util.descendantsOfType
import com.intellij.psi.util.parentOfTypes
import com.intellij.psi.util.siblings

class ArbPsiUtils {
    /**
     * the goal is to return the current key, we're working on. it's always the root key we search, i.e. on the first level of the
     * json object. If we're nested, we recursively search for the key up until there is no parent. if the key starts with an @, we just remove the @
     */
    fun getCurrentTranslationKey(element: PsiElement): String? {
        //the implementation is straight forward, we have a lookahead of 2 calls to the parent (elem?.parent?.parent == file) - is it an arb file? if yes, then we're at the desired key, if not, we move up one parent
        var currentElement = element
        while (currentElement.parent != null && currentElement.parent.parent != null) {
            val parentParent = currentElement.parent.parent
            if (parentParent is JsonFileImpl) {
                return (currentElement as JsonPropertyImpl).name.removePrefix("@")
            }
            currentElement = currentElement.parent
        }
        return null
    }

    /**
     * you provide a reference to the translation file and the key you are searching for.
     * <br/>
     * {
     *  "greeting": "hello world"
     * }
     * if you provide the key "greeting", you will get "hello world"
     */
    fun getValueOfTranslationKey(element: PsiElement, key: String): String? {
        val rootObject = getRootObject(element)
        if (rootObject != null) {
            val property = rootObject.children.filterIsInstance<JsonPropertyImpl>().find { it.name == key }
            if (property != null) {
                return property.value?.let {
                    return JsonPsiUtil.stripQuotes(it.text)
                }
            }
        }
        return null
    }

    private fun getRootObject(element: PsiElement): JsonObjectImpl? {
        // get the root element, (JsonObjectImpl) -> get the children (JsonPropertyImpl) -> filter the one with the key -> get the value,
        // but first we need to check if this or the parent is already a file
        if (element is JsonFileImpl) {
            return element.children.filterIsInstance<JsonObjectImpl>().firstOrNull()
        }

        if (element.parent is JsonFileImpl) {
            return element.parent.children.filterIsInstance<JsonObjectImpl>().firstOrNull()
        }

        var currentElement = element
        while (currentElement.parent != null && currentElement.parent.parent != null) {
            val parentParent = currentElement.parent.parent
            if (parentParent is JsonFileImpl) {
                return currentElement.parent as JsonObjectImpl
            }
            currentElement = currentElement.parent
        }
        return null
    }

    fun getAllStringArraysFromRoot(element: PsiElement): List<StringArrayContent> {
        val rootProperties = collectRootProperties(element)
        return rootProperties.map { getStringArrayContent(it) }
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

    /**
     * collect all json properties on root
     */
    private fun collectRootProperties(element: PsiElement): List<JsonPropertyImpl> {
        val containingFile = element.containingFile
        if (containingFile !is JsonFileImpl) {
            throw IllegalArgumentException("Provided element is not a json file")
        }
        return containingFile.childrenOfType<JsonObjectImpl>()
            .firstOrNull() //root object
            ?.childrenOfType<JsonPropertyImpl>() ?: emptyList()
    }

    fun getArbEntryFromKey(element: PsiElement, key: String): ArbEntry? {
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
                    val description: String? = getDescription(element);
                    val arbEntry = findCorrespondingEntry(element)
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
                var entry = findCorrespondingEntry(lastPropertyParent)
                entry?.description = description ?: ""
                entry
            } else {
                val description = findCorrespondingDescription(lastPropertyParent)
                ArbEntry(lastPropertyParent.name, JsonPsiUtil.stripQuotes(lastPropertyParent.value?.text ?: ""), description ?: "")
            }
        }
    }

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

    private fun findCorrespondingDescription(element: JsonPropertyImpl): String? {
        val nameWithPrefix = "@${element.name}"
        val maybeElem: PsiElement? = element.siblings(forward = true).find {
            it is JsonPropertyImpl && it.name == nameWithPrefix
        }
        return maybeElem?.let {
            return getDescription(it as JsonPropertyImpl)
        }
    }

    private fun findCorrespondingEntry(element: JsonPropertyImpl): ArbEntry? {
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
}