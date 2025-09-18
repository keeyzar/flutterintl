package de.keeyzar.gpthelper.gpthelper.features.shared.domain.service

interface InstallFileProvider {
    fun fileExists(path: String): Boolean
    fun readFile(path: String): String?
    fun writeFile(path: String, content: String)
}

