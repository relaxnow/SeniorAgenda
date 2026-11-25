package nl.senioragenda.app.reminder

import nl.senioragenda.app.domain.model.Appointment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderPolicyTest {

    private val date = LocalDate.of(2026, 11, 26)
    private val huisarts = Appointment.timed(
        id = "huisarts-1",
        title = "Huisarts",
        date = date,
        startTime = LocalTime.of(14, 0),
        endTime = LocalTime.of(15, 0),
    )

    @Test
    fun generatesSixTriggersStarting30MinutesBeforeEvery5Minutes() {
        val triggers = ReminderPolicy.reminderTriggersFor(huisarts)
        assertEquals(
            listOf(
                LocalDateTime.of(date, LocalTime.of(13, 30)),
                LocalDateTime.of(date, LocalTime.of(13, 35)),
                LocalDateTime.of(date, LocalTime.of(13, 40)),
                LocalDateTime.of(date, LocalTime.of(13, 45)),
                LocalDateTime.of(date, LocalTime.of(13, 50)),
                LocalDateTime.of(date, LocalTime.of(13, 55)),
            ),
            triggers,
        )
    }

    @Test
    fun doesNotTriggerBefore30MinutesLeadTime() {
        val eval = ReminderPolicy.evaluate(
            appointments = listOf(huisarts),
            now = LocalDateTime.of(date, LocalTime.of(13, 29)),
            acknowledgedIds = emptySet(),
            lastSpokenSlotById = emptyMap(),
        )
        assertNull(eval.activeAppointment)
        assertFalse(eval.shouldSpeakNow)
    }

    @Test
    fun repeatsSpeechOnEach5MinuteBoundaryUntilUnderstood() {
        val at1330 = ReminderPolicy.evaluate(
            appointments = listOf(huisarts),
            now = LocalDateTime.of(date, LocalTime.of(13, 30)),
            acknowledgedIds = emptySet(),
            lastSpokenSlotById = emptyMap(),
        )
        assertEquals(huisarts.id, at1330.activeAppointment?.id)
        assertEquals(0, at1330.currentSlotIndex)
        assertTrue(at1330.shouldSpeakNow)

        val at1333 = ReminderPolicy.evaluate(
            appointments = listOf(huisarts),
            now = LocalDateTime.of(date, LocalTime.of(13, 33)),
            acknowledgedIds = emptySet(),
            lastSpokenSlotById = mapOf(huisarts.id to 0),
        )
        assertEquals(huisarts.id, at1333.activeAppointment?.id)
        assertFalse(at1333.shouldSpeakNow)

        val at1335 = ReminderPolicy.evaluate(
            appointments = listOf(huisarts),
            now = LocalDateTime.of(date, LocalTime.of(13, 35)),
            acknowledgedIds = emptySet(),
            lastSpokenSlotById = mapOf(huisarts.id to 0),
        )
        assertEquals(1, at1335.currentSlotIndex)
        assertTrue(at1335.shouldSpeakNow)

        val afterUnderstood = ReminderPolicy.evaluate(
            appointments = listOf(huisarts),
            now = LocalDateTime.of(date, LocalTime.of(13, 40)),
            acknowledgedIds = setOf(huisarts.id),
            lastSpokenSlotById = mapOf(huisarts.id to 1),
        )
        assertNull(afterUnderstood.activeAppointment)
    }

    @Test
    fun allDayItemsDoNotGenerateTimedReminders() {
        val allDay = Appointment.allDay(
            id = "hapjes",
            title = "Hapjes komen aan",
            date = date,
        )
        assertTrue(ReminderPolicy.reminderTriggersFor(allDay).isEmpty())
    }
}
