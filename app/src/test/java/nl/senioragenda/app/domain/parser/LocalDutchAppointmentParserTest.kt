package nl.senioragenda.app.domain.parser

import nl.senioragenda.app.data.remote.GeminiAppointmentParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class LocalDutchAppointmentParserTest {

    private val referenceNow = LocalDateTime.of(2026, 11, 26, 10, 0) // donderdag

    @Test
    fun parsesExplicitDutchTimeRange() {
        val draft = LocalDutchAppointmentParser.parse(
            input = "Morgen van 10:15 tot 11:00 fysiotherapie",
            referenceNow = referenceNow,
        )
        assertNotNull(draft)
        assertEquals(LocalDate.of(2026, 11, 27), draft!!.date)
        assertFalse(draft.isAllDay)
        assertEquals(LocalTime.of(10, 15), draft.startTime)
        assertEquals(LocalTime.of(11, 0), draft.endTime)
        assertEquals("Fysiotherapie", draft.title)
    }

    @Test
    fun defaultsToOneHourDurationWhenOnlyStartTimeGiven() {
        val draft = LocalDutchAppointmentParser.parse(
            input = "Morgen om half drie tandarts",
            referenceNow = referenceNow,
        )
        assertNotNull(draft)
        assertEquals(LocalDate.of(2026, 11, 27), draft!!.date)
        assertFalse(draft.isAllDay)
        assertEquals(LocalTime.of(14, 30), draft.startTime)
        assertEquals(LocalTime.of(15, 30), draft.endTime)
        assertEquals("Tandarts", draft.title)
    }

    @Test
    fun parsesAllDayDutchAppointment() {
        val draft = LocalDutchAppointmentParser.parse(
            input = "Zaterdag hele dag hapjes komen aan",
            referenceNow = referenceNow,
        )
        assertNotNull(draft)
        assertEquals(LocalDate.of(2026, 11, 28), draft!!.date)
        assertTrue(draft.isAllDay)
        assertNull(draft.startTime)
        assertNull(draft.endTime)
        assertEquals("Hapjes komen aan", draft.title)
    }

    @Test
    fun parsesSpokenDutchAfternoonRangeWithoutUur() {
        val draft = LocalDutchAppointmentParser.parse(
            input = "Van 2 's middags tot 3 's middags huisarts",
            referenceNow = referenceNow,
        )
        assertNotNull(draft)
        assertFalse(draft!!.isAllDay)
        assertEquals(LocalTime.of(14, 0), draft.startTime)
        assertEquals(LocalTime.of(15, 0), draft.endTime)
        assertEquals("Huisarts", draft.title)
    }

    @Test
    fun geminiJsonParserAppliesOneHourDefaultWhenEndTimeMissing() {
        val parser = GeminiAppointmentParser(apiKey = "")
        val draft = parser.parseStructuredJson(
            """{"title":"Huisarts","date":"2026-11-27","isAllDay":false,"startTime":"14:00","endTime":null}""",
        )
        assertNotNull(draft)
        assertEquals(LocalTime.of(14, 0), draft!!.startTime)
        assertEquals(LocalTime.of(15, 0), draft.endTime)
        assertTrue(draft.parsedByGemini)
    }
}
