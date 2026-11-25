package nl.senioragenda.app.domain.model

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Kernmodel voor een afspraak in het dagoverzicht.
 *
 * Regels:
 * - Een tijdsgebonden afspraak heeft altijd zowel een starttijd als een eindtijd.
 * - Een hele-dag-item heeft [isAllDay] = true en kan op dezelfde dag naast tijdsgebonden afspraken staan.
 */
data class Appointment(
    val id: String,
    val title: String,
    val date: LocalDate,
    val isAllDay: Boolean,
    val startTime: LocalTime? = null,
    val endTime: LocalTime? = null,
    val location: String? = null,
    val source: AppointmentSource = AppointmentSource.LOCAL,
) {
    init {
        require(title.isNotBlank()) { "Titel van een afspraak mag niet leeg zijn." }
        if (isAllDay) {
            require(startTime == null && endTime == null) {
                "Een hele-dag-afspraak mag geen start- of eindtijd hebben."
            }
        } else {
            require(startTime != null && endTime != null) {
                "Een tijdsgebonden afspraak moet altijd een starttijd en eindtijd hebben."
            }
        }
    }

    val startDateTime: LocalDateTime?
        get() = startTime?.let { LocalDateTime.of(date, it) }

    val endDateTime: LocalDateTime?
        get() = endTime?.let { end ->
            val start = startTime ?: return null
            if (end.isAfter(start)) {
                LocalDateTime.of(date, end)
            } else {
                LocalDateTime.of(date.plusDays(1), end)
            }
        }

    companion object {
        fun allDay(
            id: String,
            title: String,
            date: LocalDate,
            location: String? = null,
            source: AppointmentSource = AppointmentSource.LOCAL,
        ): Appointment = Appointment(
            id = id,
            title = title.trim(),
            date = date,
            isAllDay = true,
            startTime = null,
            endTime = null,
            location = location?.takeIf { it.isNotBlank() },
            source = source,
        )

        fun timed(
            id: String,
            title: String,
            date: LocalDate,
            startTime: LocalTime,
            endTime: LocalTime? = null,
            location: String? = null,
            source: AppointmentSource = AppointmentSource.LOCAL,
        ): Appointment {
            val resolvedEnd = endTime?.takeIf { it != startTime } ?: defaultEndTime(startTime)
            return Appointment(
                id = id,
                title = title.trim(),
                date = date,
                isAllDay = false,
                startTime = startTime,
                endTime = resolvedEnd,
                location = location?.takeIf { it.isNotBlank() },
                source = source,
            )
        }

        /**
         * Standaardduur van 1 uur wanneer geen eindtijd is opgegeven.
         * Blijft op dezelfde dag (maximaal 23:59) om middernacht-overloop op het bord te voorkomen.
         */
        fun defaultEndTime(startTime: LocalTime): LocalTime {
            return if (startTime.hour >= 23) {
                LocalTime.of(23, 59)
            } else {
                startTime.plusHours(1)
            }
        }

        val CHRONOLOGICAL_COMPARATOR: Comparator<Appointment> = compareBy<Appointment>(
            { it.date },
            { !it.isAllDay },
            { it.startTime ?: LocalTime.MIN },
            { it.endTime ?: LocalTime.MIN },
            { it.title.lowercase() },
        )
    }
}

enum class AppointmentSource {
    GOOGLE_CALENDAR,
    LOCAL,
    SAMPLE,
}

data class DayAgenda(
    val date: LocalDate,
    val appointments: List<Appointment>,
) {
    val allDayItems: List<Appointment>
        get() = appointments.filter { it.isAllDay }

    val timedItems: List<Appointment>
        get() = appointments.filter { !it.isAllDay }

    val isEmpty: Boolean
        get() = appointments.isEmpty()
}

data class AgendaSnapshot(
    val today: DayAgenda,
    val tomorrow: DayAgenda,
    val upcomingDays: List<DayAgenda>,
)

data class ParsedAppointmentDraft(
    val title: String,
    val date: LocalDate,
    val isAllDay: Boolean,
    val startTime: LocalTime?,
    val endTime: LocalTime?,
    val parsedByGemini: Boolean,
) {
    fun toAppointment(id: String, source: AppointmentSource): Appointment {
        return if (isAllDay) {
            Appointment.allDay(id = id, title = title, date = date, source = source)
        } else {
            val start = requireNotNull(startTime) { "Starttijd ontbreekt." }
            Appointment.timed(
                id = id,
                title = title,
                date = date,
                startTime = start,
                endTime = endTime,
                source = source,
            )
        }
    }
}
