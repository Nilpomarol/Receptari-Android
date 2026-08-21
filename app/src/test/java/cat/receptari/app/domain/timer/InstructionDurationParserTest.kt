package cat.receptari.app.domain.timer

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstructionDurationParserTest {

    @Test
    fun `recognizes Catalan Spanish and English minutes`() {
        assertEquals(10, InstructionDurationParser.parseMinutes("Cou-ho durant 10 minuts."))
        assertEquals(25, InstructionDurationParser.parseMinutes("Cocina durante 25 minutos."))
        assertEquals(15, InstructionDurationParser.parseMinutes("Bake for 15 minutes."))
    }

    @Test
    fun `combines hours and minutes`() {
        assertEquals(90, InstructionDurationParser.parseMinutes("Deixa-ho 1 hora i 30 minuts."))
        assertEquals(90, InstructionDurationParser.parseMinutes("Rest for 1 hour 30 min."))
        assertEquals(90, InstructionDurationParser.parseMinutes("Hornea 1,5 horas."))
    }

    @Test
    fun `rejects ambiguous ranges and missing durations`() {
        assertNull(InstructionDurationParser.parseMinutes("Cook for 10–12 minutes."))
        assertNull(InstructionDurationParser.parseMinutes("Escalfa el forn a 180 graus."))
    }

    @Test
    fun `detects a duration mention including ambiguous ranges`() {
        assertTrue(InstructionDurationParser.containsDuration("Bake for 15 minutes."))
        assertTrue(InstructionDurationParser.containsDuration("Cook for 10-12 minutes."))
        assertTrue(InstructionDurationParser.containsDuration("Deixa-ho 1 hora i 30 minuts."))
    }

    @Test
    fun `reports no duration when the step never mentions time`() {
        assertFalse(InstructionDurationParser.containsDuration("Escalfa el forn a 180 graus."))
        assertFalse(InstructionDurationParser.containsDuration("Pica la ceba ben fina."))
        assertFalse(InstructionDurationParser.containsDuration("Wait 25 hours."))
    }

    @Test
    fun `rejects zero and durations beyond one day`() {
        assertNull(InstructionDurationParser.parseMinutes("Wait 0 minutes."))
        assertNull(InstructionDurationParser.parseMinutes("Wait 25 hours."))
    }
}
