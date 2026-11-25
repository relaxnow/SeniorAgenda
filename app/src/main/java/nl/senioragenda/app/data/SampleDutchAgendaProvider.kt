package nl.senioragenda.app.data

import nl.senioragenda.app.domain.model.Appointment
import nl.senioragenda.app.domain.model.AppointmentSource
import java.time.LocalDate
import java.time.LocalTime

/**
 * Realistische Nederlandse voorbeeldafspraken voor de eerste opstart vóórdat Google Agenda
 * is gekoppeld, zodat het hele dashboard (hele-dag-items, meerdere afspraken per dag,
 * morgen en de komende vier dagen) direct te testen is op de Galaxy Tab S9 FE.
 */
object SampleDutchAgendaProvider {

    fun createSampleAppointments(today: LocalDate): List<Appointment> {
        val tomorrow = today.plusDays(1)
        val dayPlus2 = today.plusDays(2)
        val dayPlus3 = today.plusDays(3)
        val dayPlus5 = today.plusDays(5)

        return listOf(
            Appointment.allDay(
                id = "sample-allday-today",
                title = "Hapjes komen aan",
                date = today,
                source = AppointmentSource.SAMPLE,
            ),
            Appointment.timed(
                id = "sample-today-1",
                title = "Fysiotherapie",
                date = today,
                startTime = LocalTime.of(10, 15),
                endTime = LocalTime.of(11, 0),
                source = AppointmentSource.SAMPLE,
            ),
            Appointment.timed(
                id = "sample-today-2",
                title = "Bezoek van de familie",
                date = today,
                startTime = LocalTime.of(12, 30),
                endTime = LocalTime.of(16, 30),
                source = AppointmentSource.SAMPLE,
            ),
            Appointment.timed(
                id = "sample-tomorrow-1",
                title = "Huisarts",
                date = tomorrow,
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 0),
                source = AppointmentSource.SAMPLE,
            ),
            Appointment.timed(
                id = "sample-day2-1",
                title = "Koffie bij de buren",
                date = dayPlus2,
                startTime = LocalTime.of(10, 30),
                endTime = LocalTime.of(11, 30),
                source = AppointmentSource.SAMPLE,
            ),
            Appointment.allDay(
                id = "sample-day3-allday",
                title = "Verjaardag kleindochter",
                date = dayPlus3,
                source = AppointmentSource.SAMPLE,
            ),
            Appointment.timed(
                id = "sample-day5-1",
                title = "Kapper aan huis",
                date = dayPlus5,
                startTime = LocalTime.of(13, 30),
                endTime = LocalTime.of(14, 30),
                source = AppointmentSource.SAMPLE,
            ),
        ).sortedWith(Appointment.CHRONOLOGICAL_COMPARATOR)
    }
}
