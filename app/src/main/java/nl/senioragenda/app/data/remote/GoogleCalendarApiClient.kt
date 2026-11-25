package nl.senioragenda.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.senioragenda.app.domain.model.Appointment
import nl.senioragenda.app.domain.model.AppointmentSource
import nl.senioragenda.app.domain.model.ParsedAppointmentDraft
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Rechtstreekse REST v3-client voor Google Calendar (ophalen en toevoegen van afspraken).
 * Ondersteunt zowel hele-dag-items als tijdsgebonden afspraken met begin- en eindtijd.
 */
class GoogleCalendarApiClient(
    private val httpClient: OkHttpClient = defaultHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = false
    }

    suspend fun fetchEvents(
        accessToken: String,
        calendarId: String,
        fromDate: LocalDate,
        toDateInclusive: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): List<Appointment> = withContext(Dispatchers.IO) {
        val encodedCalendarId = URLEncoder.encode(calendarId, StandardCharsets.UTF_8.name())
        val timeMin = fromDate.atStartOfDay(zoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val timeMax = toDateInclusive.plusDays(1)
            .atStartOfDay(zoneId)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)

        val url = buildString {
            append(BASE_URL)
            append("/calendars/")
            append(encodedCalendarId)
            append("/events?singleEvents=true&orderBy=startTime")
            append("&timeMin=").append(URLEncoder.encode(timeMin, StandardCharsets.UTF_8.name()))
            append("&timeMax=").append(URLEncoder.encode(timeMax, StandardCharsets.UTF_8.name()))
            append("&maxResults=100")
        }

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .get()
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Google Agenda gaf statuscode ${response.code} terug.")
            }
            val body = response.body?.string().orEmpty()
            val payload = json.decodeFromString<GoogleEventsResponse>(body)
            payload.items
                .filter { it.status != "cancelled" }
                .flatMap { event ->
                    mapEventToAppointments(
                        event = event,
                        fromDate = fromDate,
                        toDateInclusive = toDateInclusive,
                        zoneId = zoneId,
                    )
                }
                .sortedWith(Appointment.CHRONOLOGICAL_COMPARATOR)
        }
    }

    suspend fun createEvent(
        accessToken: String,
        calendarId: String,
        draft: ParsedAppointmentDraft,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Appointment = withContext(Dispatchers.IO) {
        val encodedCalendarId = URLEncoder.encode(calendarId, StandardCharsets.UTF_8.name())
        val url = "$BASE_URL/calendars/$encodedCalendarId/events"

        val bodyPayload = if (draft.isAllDay) {
            GoogleCreateEventRequest(
                summary = draft.title,
                start = GoogleEventDateTime(date = draft.date.toString()),
                end = GoogleEventDateTime(date = draft.date.plusDays(1).toString()),
            )
        } else {
            val start = requireNotNull(draft.startTime)
            val end = draft.endTime ?: Appointment.defaultEndTime(start)
            val startZoned = draft.date.atTime(start).atZone(zoneId)
            val endDate = if (end.isAfter(start)) draft.date else draft.date.plusDays(1)
            val endZoned = endDate.atTime(end).atZone(zoneId)

            GoogleCreateEventRequest(
                summary = draft.title,
                start = GoogleEventDateTime(
                    dateTime = startZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    timeZone = zoneId.id,
                ),
                end = GoogleEventDateTime(
                    dateTime = endZoned.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
                    timeZone = zoneId.id,
                ),
            )
        }

        val requestBody = json.encodeToString(bodyPayload).toRequestBody(JSON_MEDIA_TYPE)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $accessToken")
            .post(requestBody)
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Opslaan in Google Agenda mislukt (code ${response.code}).")
            }
            val body = response.body?.string().orEmpty()
            val created = json.decodeFromString<GoogleEventDto>(body)
            val createdId = created.id ?: "gcal-${System.currentTimeMillis()}"
            draft.toAppointment(id = createdId, source = AppointmentSource.GOOGLE_CALENDAR)
        }
    }

    internal fun mapEventToAppointments(
        event: GoogleEventDto,
        fromDate: LocalDate,
        toDateInclusive: LocalDate,
        zoneId: ZoneId,
    ): List<Appointment> {
        val title = event.summary?.trim()?.takeIf { it.isNotBlank() } ?: "Afspraak zonder titel"
        val baseId = event.id ?: "${title.hashCode()}-${event.start?.date ?: event.start?.dateTime}"

        val startDateText = event.start?.date
        if (startDateText != null) {
            val startDate = LocalDate.parse(startDateText)
            val exclusiveEndDate = event.end?.date?.let(LocalDate::parse) ?: startDate.plusDays(1)
            val candidateLast = exclusiveEndDate.minusDays(1)
            val lastInclusive = if (candidateLast.isBefore(startDate)) startDate else candidateLast

            val results = mutableListOf<Appointment>()
            var cursor = if (startDate.isAfter(fromDate)) startDate else fromDate
            val limit = if (lastInclusive.isBefore(toDateInclusive)) lastInclusive else toDateInclusive
            while (!cursor.isAfter(limit)) {
                results += Appointment.allDay(
                    id = "$baseId-$cursor",
                    title = title,
                    date = cursor,
                    location = event.location,
                    source = AppointmentSource.GOOGLE_CALENDAR,
                )
                cursor = cursor.plusDays(1)
            }
            return results
        }

        val startDateTimeText = event.start?.dateTime ?: return emptyList()
        val startLocal = OffsetDateTime.parse(startDateTimeText).atZoneSameInstant(zoneId).toLocalDateTime()
        val endLocal = event.end?.dateTime
            ?.let { OffsetDateTime.parse(it).atZoneSameInstant(zoneId).toLocalDateTime() }
            ?: startLocal.plusHours(1)

        val eventDate = startLocal.toLocalDate()
        if (eventDate.isBefore(fromDate) || eventDate.isAfter(toDateInclusive)) {
            return emptyList()
        }

        return listOf(
            Appointment.timed(
                id = baseId,
                title = title,
                date = eventDate,
                startTime = startLocal.toLocalTime(),
                endTime = endLocal.toLocalTime(),
                location = event.location,
                source = AppointmentSource.GOOGLE_CALENDAR,
            ),
        )
    }

    @Serializable
    internal data class GoogleEventsResponse(
        val items: List<GoogleEventDto> = emptyList(),
    )

    @Serializable
    internal data class GoogleEventDto(
        val id: String? = null,
        val status: String? = null,
        val summary: String? = null,
        val location: String? = null,
        val start: GoogleEventDateTime? = null,
        val end: GoogleEventDateTime? = null,
    )

    @Serializable
    internal data class GoogleCreateEventRequest(
        val summary: String,
        val start: GoogleEventDateTime,
        val end: GoogleEventDateTime,
    )

    @Serializable
    internal data class GoogleEventDateTime(
        val date: String? = null,
        val dateTime: String? = null,
        val timeZone: String? = null,
    )

    companion object {
        private const val BASE_URL = "https://www.googleapis.com/calendar/v3"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }
}
