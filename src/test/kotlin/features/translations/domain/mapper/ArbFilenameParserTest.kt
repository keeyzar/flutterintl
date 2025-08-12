package de.keeyzar.gpthelper.gpthelper.features.translations.domain.mapper

import de.keeyzar.gpthelper.gpthelper.features.translations.domain.parser.ArbFilenameParser
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class ArbFilenameParserTest {
    private val sut = ArbFilenameParser()

    @Test
    fun `getArbFilePrefix returns correct prefix`() {
        assertEquals("ABC", sut.getArbFilePrefix("ABCen_US.arb"))
        assertEquals("AB_", sut.getArbFilePrefix("AB_en.arb"))
        assertEquals("qw_", sut.getArbFilePrefix("qw_de.arb"))
        assertEquals("q123_", sut.getArbFilePrefix("q123_de_DE.arb"))
        assertEquals("", sut.getArbFilePrefix("test"))
        assertEquals("", sut.getArbFilePrefix(""))
    }
}
