package de.keeyzar.gpthelper.gpthelper.features.setup.domain.service

/**
 * Abstraktion für das Finden und Modifizieren von App-Referenzen (MaterialApp/CupertinoApp).
 * Die Referenz ist ein generisches Object, die konkrete Implementierung kann z.B. PsiElement verwenden.
 */
interface AppReferenceProvider {
    /**
     * Sucht alle MaterialApp- und CupertinoApp-Referenzen in .dart-Dateien unterhalb von lib.
     * @return Liste von Referenz-Objekten (z.B. PsiElement)
     */
    fun findAppReferences(): List<Any>

    /**
     * Modifiziert die gegebene App-Referenz, um Lokalisierung zu aktivieren.
     * @param reference Die zu modifizierende Referenz (z.B. PsiElement)
     * @return true, wenn erfolgreich
     */
    fun enableLocalization(reference: Any): Boolean

    fun enableLocalizationOnDummy(reference: Any): String?

    fun getContent(reference: Any): String?

    fun modifyFileContent(reference: Any, newContent: String)

    /**
     * Prüft, ob die gegebene Referenz bereits die für Lokalisierung benötigten Argumente enthält.
     * Wird von SetupService.isProjectCombined verwendet.
     */
    fun referenceHasLocalization(reference: Any): Boolean
}
