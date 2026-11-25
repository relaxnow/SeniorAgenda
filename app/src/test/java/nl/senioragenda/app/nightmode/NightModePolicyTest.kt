package nl.senioragenda.app.nightmode

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class NightModePolicyTest {

    private val date = LocalDate.of(2026, 11, 26)

    @Test
    fun checks2330To0530ScheduleBoundaries() {
        assertFalse(NightModePolicy.isInNightSchedule(LocalTime.of(23, 29)))
        assertTrue(NightModePolicy.isInNightSchedule(LocalTime.of(23, 30)))
        assertTrue(NightModePolicy.isInNightSchedule(LocalTime.of(2, 0)))
        assertTrue(NightModePolicy.isInNightSchedule(LocalTime.of(5, 29)))
        assertFalse(NightModePolicy.isInNightSchedule(LocalTime.of(5, 30)))
    }

    @Test
    fun temporaryWakeLastsUntilDeadlineAndThenReturnsToSleeping() {
        val tapTime = LocalDateTime.of(date, LocalTime.of(23, 45, 0))
        val wakeUntil = tapTime.plusSeconds(NightModePolicy.TEMPORARY_WAKE_SECONDS)

        val duringWake = NightModePolicy.resolve(
            now = tapTime.plusSeconds(10),
            hasActiveReminder = false,
            temporaryWakeUntil = wakeUntil,
        )
        assertTrue(duringWake is NightModeState.TemporaryWake)
        assertEquals(20L, (duringWake as NightModeState.TemporaryWake).secondsRemaining)

        val afterWake = NightModePolicy.resolve(
            now = wakeUntil,
            hasActiveReminder = false,
            temporaryWakeUntil = wakeUntil,
        )
        assertEquals(NightModeState.Sleeping, afterWake)
        assertEquals(NightModePolicy.LCD_MINIMUM_BRIGHTNESS, afterWake.targetScreenBrightness, 0.0001f)
    }

    @Test
    fun activeReminderAlwaysOverridesNightMode() {
        val state = NightModePolicy.resolve(
            now = LocalDateTime.of(date, LocalTime.of(1, 30)),
            hasActiveReminder = true,
            temporaryWakeUntil = null,
        )
        assertEquals(NightModeState.ReminderWakeOverride, state)
        assertEquals(NightModePolicy.REMINDER_OVERRIDE_BRIGHTNESS, state.targetScreenBrightness, 0.0001f)
    }
}
