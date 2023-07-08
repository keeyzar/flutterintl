package de.keeyzar.gpthelper.gpthelper.features.shared.infrastructure.model

import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class UserSettingsTest {

    val sut = UserSettings(
        "arbDir",
        "outputClass",
        false,
        "templateArbFile",
        "intlConfigFile",
        true,
        "outputLocalizationFile",
        3
    )

    @Test
    fun `getArbFilePrefix returns correct prefix`() {
        assertEquals("ABC", sut.copy(templateArbFile = "ABCen_US.arb").getArbFilePrefix())
        assertEquals("AB_", sut.copy(templateArbFile = "AB_en.arb").getArbFilePrefix())
        assertEquals("qw_", sut.copy(templateArbFile = "qw_de.arb").getArbFilePrefix())
        assertEquals("q123_", sut.copy(templateArbFile = "q123_de_DE.arb").getArbFilePrefix())
        assertEquals("", sut.copy(templateArbFile = "test").getArbFilePrefix())
        assertEquals("", sut.copy(templateArbFile = "").getArbFilePrefix())
    }
}
