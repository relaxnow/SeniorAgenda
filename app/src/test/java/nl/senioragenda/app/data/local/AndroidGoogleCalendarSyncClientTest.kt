package nl.senioragenda.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset

class AndroidGoogleCalendarSyncClientTest {

    private val amsterdam = ZoneId.of("Europe/Amsterdam")

    @Test
    fun mapsUtcAllDayEventWithoutDayShiftInAmsterdamTimezone() {
        val targetDate = LocalDate.of(2026, 11, 26)
        val beginUtc = targetDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val endUtc = targetDate.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

        val mapped = AndroidGoogleCalendarSyncClient.mapInstanceToAppointments(
            eventId = 42L,
            title = "Hapjes komen aan",
            beginMillis = beginUtc,
            endMillis = endUtc,
            isAllDay = true,
            location = null,
            fromDate = targetDate,
            toDateInclusive = targetDate.plusDays(5),
            zoneId = amsterdam,
        )

        assertEquals(1, mapped.size)
        assertTrue(mapped.first().isAllDay)
        assertEquals(targetDate, mapped.first().date)
        assertEquals("Hapjes komen aan", mapped.first().title)
    }

    @Test
    fun mapsTimedEventInAmsterdamTimezone() {
        val date = LocalDate.of(2026, 11, 26)
        val beginMillis = date.atTime(LocalTime.of(10, 15)).atZone(amsterdam).toInstant().toEpochMilli()
        val endMillis = date.atTime(LocalTime.of(11, 0)).atZone(amsterdam).toInstant().toEpochMilli()

        val mapped = AndroidGoogleCalendarSyncClient.mapInstanceToAppointments(
            eventId = 99L,
            title = "Fysio Ellen",
            beginMillis = beginMillis,
            endMillis = endMillis,
            isAllDay = false,
            location = null,
            fromDate = date,
            toDateInclusive = date.plusDays(5),
            zoneId = amsterdam,
        )

        assertEquals(1, mapped.size)
        assertFalse(mapped.first().isAllDay)
        assertEquals(LocalTime.of(10, 15), mapped.first().startTime)
        assertEquals(LocalTime.of(11, 0), mapped.first().endTime)
    }

    @Test
    fun filtersOutHolidayCalendars() {
        assertTrue(
            AndroidGoogleCalendarSyncClient.isHolidayCalendar(
                ownerAccount = "nl.dutch#holiday@group.v.calendar.google.com",
                displayName = "Feestdagen in Nederland",
            ),
        )
        assertTrue(
            AndroidGoogleCalendarSyncClient.isHolidayCalendar(
                ownerAccount = "en.dutch#holiday@group.v.calendar.google.com",
                displayName = "Holidays in Netherlands",
            ),
        )
        assertFalse(
            AndroidGoogleCalendarSyncClient.isHolidayCalendar(
                ownerAccount = "baukemaben@gmail.com",
                displayName = "baukemaben@gmail.com",
            ),
        )
    }
}
