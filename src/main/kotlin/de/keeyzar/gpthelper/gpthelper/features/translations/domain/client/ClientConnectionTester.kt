package de.keeyzar.gpthelper.gpthelper.features.translations.domain.client

/**
 * should be used to find out whether the client has a connection
 */
fun interface ClientConnectionTester {
    suspend fun testClientConnection(key: String): Throwable?
}
