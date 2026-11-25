package nl.senioragenda.app.domain.formatting

import nl.senioragenda.app.domain.model.Appointment
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

class DutchDateTimeFormatterTest {

    @Test
    fun formatsTopBarDateInDutchUpperCase() {
        val date = LocalDate.of(2026, 11, 26) // donderdag
        assertEquals("DONDERDAG 26 NOVEMBER", DutchDateTimeFormatter.formatTopBarDate(date))
    }

    @Test
    fun formatsTomorrowHeaderInDutch() {
        val date = LocalDate.of(2026, 11, 27) // vrijdag
        assertEquals("MORGEN - VRIJDAG 27 NOVEMBER", DutchDateTimeFormatter.formatTomorrowHeader(date))
    }

    @Test
    fun formatsCompactDayInDutch() {
        val date = LocalDate.of(2026, 11, 28) // zaterdag
        assertEquals("Za 28 nov", DutchDateTimeFormatter.formatCompactDay(date))
    }

    @Test
    fun formats24HourTimeRangeAndAllDayLabel() {
        val date = LocalDate.of(2026, 11, 26)
        val timed = Appointment.timed(
            id = "1",
            title = "Fysiotherapie",
            date = date,
            startTime = LocalTime.of(10, 15),
            endTime = LocalTime.of(11, 0),
        )
        val allDay = Appointment.allDay(
            id = "2",
            title = "Hapjes komen aan",
            date = date,
        )

        assertEquals("10:15 - 11:00", DutchDateTimeFormatter.formatAppointmentTimeLabel(timed))
        assertEquals("HELE DAG", DutchDateTimeFormatter.formatAppointmentTimeLabel(allDay))
    }
}
