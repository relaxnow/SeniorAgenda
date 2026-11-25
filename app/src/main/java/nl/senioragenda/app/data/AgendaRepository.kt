package nl.senioragenda.app.data

import nl.senioragenda.app.data.local.AgendaPreferences
import nl.senioragenda.app.data.local.AndroidGoogleCalendarSyncClient
import nl.senioragenda.app.data.local.DeviceCalendarLoadResult
import nl.senioragenda.app.data.remote.GoogleCalendarApiClient
import nl.senioragenda.app.domain.model.AgendaSnapshot
import nl.senioragenda.app.domain.model.Appointment
import nl.senioragenda.app.domain.model.AppointmentSource
import nl.senioragenda.app.domain.model.DayAgenda
import nl.senioragenda.app.domain.model.ParsedAppointmentDraft
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Centrale opslag- en synchronisatielaag tussen:
 * 1. De op de tablet gesynchroniseerde Google Agenda (`CalendarContract`, standaard & direct live),
 * 2. Rechtstreekse Google Calendar REST API v3 (OAuth), en
 * 3. Lokale voorbeeldweergave (alleen wanneer er nog geen agendatoestemming is verleend).
 */
class AgendaRepository(
    private val preferences: AgendaPreferences,
    private val androidGoogleCalendarSyncClient: AndroidGoogleCalendarSyncClient,
    private val googleCalendarApiClient: GoogleCalendarApiClient,
) {
    private val localFallbackAppointments = mutableListOf<Appointment>()
    private var lastSeedDate: LocalDate? = null

    fun hasDeviceCalendarPermission(): Boolean =
        androidGoogleCalendarSyncClient.hasReadPermission()

    fun loadLocalFallbackAppointments(today: LocalDate): List<Appointment> {
        if (lastSeedDate != today && localFallbackAppointments.none { it.source == AppointmentSource.LOCAL }) {
            localFallbackAppointments.clear()
            localFallbackAppointments.addAll(SampleDutchAgendaProvider.createSampleAppointments(today))
            lastSeedDate = today
        } else if (lastSeedDate == null) {
            localFallbackAppointments.addAll(SampleDutchAgendaProvider.createSampleAppointments(today))
            lastSeedDate = today
        }
        return localFallbackAppointments.sortedWith(Appointment.CHRONOLOGICAL_COMPARATOR)
    }

    suspend fun fetchSyncedDeviceGoogleCalendar(
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): DeviceCalendarLoadResult {
        return androidGoogleCalendarSyncClient.fetchSyncedGoogleAppointments(
            fromDate = today,
            toDateInclusive = today.plusDays(UPCOMING_DAYS_COUNT.toLong() + 1L),
            zoneId = zoneId,
        )
    }

    suspend fun fetchGoogleCalendarAppointments(
        accessToken: String,
        today: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Appointment> {
        val calendarId = preferences.calendarId
        return googleCalendarApiClient.fetchEvents(
            accessToken = accessToken,
            calendarId = calendarId,
            fromDate = today,
            toDateInclusive = today.plusDays(UPCOMING_DAYS_COUNT.toLong() + 1L),
            zoneId = zoneId,
        )
    }

    suspend fun addAppointment(
        draft: ParsedAppointmentDraft,
        accessToken: String?,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Appointment {
        if (!accessToken.isNullOrBlank()) {
            return googleCalendarApiClient.createEvent(
                accessToken = accessToken,
                calendarId = preferences.calendarId,
                draft = draft,
                zoneId = zoneId,
            )
        }

        if (androidGoogleCalendarSyncClient.hasWritePermission()) {
            val inserted = androidGoogleCalendarSyncClient.insertAppointmentIntoGoogleCalendar(
                draft = draft,
                zoneId = zoneId,
            )
            if (inserted != null) {
                return inserted
            }
        }

        val localAppointment = draft.toAppointment(
            id = "local-${UUID.randomUUID()}",
            source = AppointmentSource.LOCAL,
        )
        localFallbackAppointments.add(localAppointment)
        return localAppointment
    }

    companion object {
        const val UPCOMING_DAYS_COUNT = 4

        fun buildSnapshot(
            today: LocalDate,
            appointments: List<Appointment>,
        ): AgendaSnapshot {
            val grouped = appointments
                .sortedWith(Appointment.CHRONOLOGICAL_COMPARATOR)
                .groupBy { it.date }

            val todayAgenda = DayAgenda(
                date = today,
                appointments = grouped[today].orEmpty(),
            )
            val tomorrowDate = today.plusDays(1)
            val tomorrowAgenda = DayAgenda(
                date = tomorrowDate,
                appointments = grouped[tomorrowDate].orEmpty(),
            )
            val upcoming = (2..(UPCOMING_DAYS_COUNT + 1)).map { offset ->
                val date = today.plusDays(offset.toLong())
                DayAgenda(
                    date = date,
                    appointments = grouped[date].orEmpty(),
                )
            }

            return AgendaSnapshot(
                today = todayAgenda,
                tomorrow = tomorrowAgenda,
                upcomingDays = upcoming,
            )
        }
    }
}
