package nl.senioragenda.app.data.local

import android.Manifest
import android.accounts.Account
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import nl.senioragenda.app.domain.model.Appointment
import nl.senioragenda.app.domain.model.AppointmentSource
import nl.senioragenda.app.domain.model.ParsedAppointmentDraft
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

data class SyncedGoogleCalendar(
    val id: Long,
    val accountName: String,
    val displayName: String,
    val ownerAccount: String,
    val isPrimary: Boolean,
    val isWritable: Boolean,
)

data class DeviceCalendarLoadResult(
    val accountLabel: String?,
    val appointments: List<Appointment>,
    val hasSyncedGoogleCalendars: Boolean,
)

/**
 * Leest en schrijft rechtstreeks naar de gesynchroniseerde Google Agenda's op de Android-tablet
 * via [CalendarContract]. Hierdoor werken zowel de eigen Google Agenda als gedeelde familieagenda's
 * direct tweerichtingsverkeer zonder handmatige Google Cloud OAuth-configuratie.
 */
class AndroidGoogleCalendarSyncClient(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val contentResolver: ContentResolver = appContext.contentResolver

    fun hasReadPermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.READ_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED

    fun hasWritePermission(): Boolean = ContextCompat.checkSelfPermission(
        appContext,
        Manifest.permission.WRITE_CALENDAR,
    ) == PackageManager.PERMISSION_GRANTED

    suspend fun fetchSyncedGoogleAppointments(
        fromDate: LocalDate,
        toDateInclusive: LocalDate,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): DeviceCalendarLoadResult = withContext(Dispatchers.IO) {
        if (!hasReadPermission()) {
            return@withContext DeviceCalendarLoadResult(
                accountLabel = null,
                appointments = emptyList(),
                hasSyncedGoogleCalendars = false,
            )
        }

        val googleCalendars = querySyncedGoogleCalendars()
        if (googleCalendars.isEmpty()) {
            return@withContext DeviceCalendarLoadResult(
                accountLabel = null,
                appointments = emptyList(),
                hasSyncedGoogleCalendars = false,
            )
        }

        val targetCalendarIds = googleCalendars.map { it.id }.toSet()
        val primaryAccount = googleCalendars.firstOrNull { it.isPrimary }?.accountName
            ?: googleCalendars.first().accountName

        // Vraag iets ruimer op (±1 dag) zodat hele-dag-items in UTC nooit worden afgesneden
        val queryStartMillis = fromDate.minusDays(1)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()
        val queryEndMillis = toDateInclusive.plusDays(2)
            .atStartOfDay(zoneId)
            .toInstant()
            .toEpochMilli()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon().apply {
            ContentUris.appendId(this, queryStartMillis)
            ContentUris.appendId(this, queryEndMillis)
        }.build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.CALENDAR_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.STATUS,
        )

        val appointments = mutableListOf<Appointment>()
        contentResolver.query(
            uri,
            projection,
            "${CalendarContract.Events.DELETED} = 0",
            null,
            "${CalendarContract.Instances.BEGIN} ASC",
        )?.use { cursor ->
            val idxEventId = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val idxCalendarId = cursor.getColumnIndexOrThrow(CalendarContract.Instances.CALENDAR_ID)
            val idxTitle = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val idxBegin = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val idxEnd = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val idxAllDay = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
            val idxLocation = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val idxStatus = cursor.getColumnIndexOrThrow(CalendarContract.Instances.STATUS)

            while (cursor.moveToNext()) {
                val calendarId = cursor.getLong(idxCalendarId)
                if (calendarId !in targetCalendarIds) continue

                val status = cursor.getInt(idxStatus)
                if (status == CalendarContract.Events.STATUS_CANCELED) continue

                val eventId = cursor.getLong(idxEventId)
                val title = cursor.getString(idxTitle)?.trim().orEmpty().ifBlank { "Afspraak zonder titel" }
                val beginMillis = cursor.getLong(idxBegin)
                val endMillis = cursor.getLong(idxEnd)
                val isAllDay = cursor.getInt(idxAllDay) == 1
                val location = cursor.getString(idxLocation)

                appointments += mapInstanceToAppointments(
                    eventId = eventId,
                    title = title,
                    beginMillis = beginMillis,
                    endMillis = endMillis,
                    isAllDay = isAllDay,
                    location = location,
                    fromDate = fromDate,
                    toDateInclusive = toDateInclusive,
                    zoneId = zoneId,
                )
            }
        }

        DeviceCalendarLoadResult(
            accountLabel = primaryAccount,
            appointments = appointments
                .distinctBy { "${it.id}-${it.date}-${it.startTime}-${it.title}" }
                .sortedWith(Appointment.CHRONOLOGICAL_COMPARATOR),
            hasSyncedGoogleCalendars = true,
        )
    }

    suspend fun insertAppointmentIntoGoogleCalendar(
        draft: ParsedAppointmentDraft,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ): Appointment? = withContext(Dispatchers.IO) {
        if (!hasWritePermission()) return@withContext null

        val writableCalendar = querySyncedGoogleCalendars()
            .filter { it.isWritable }
            .sortedByDescending { it.isPrimary }
            .firstOrNull()
            ?: return@withContext null

        val values = ContentValues().apply {
            put(CalendarContract.Events.CALENDAR_ID, writableCalendar.id)
            put(CalendarContract.Events.TITLE, draft.title)
            if (draft.isAllDay) {
                val startUtc = draft.date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                val endUtc = draft.date.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                put(CalendarContract.Events.DTSTART, startUtc)
                put(CalendarContract.Events.DTEND, endUtc)
                put(CalendarContract.Events.ALL_DAY, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "UTC")
            } else {
                val start = requireNotNull(draft.startTime)
                val end = draft.endTime ?: Appointment.defaultEndTime(start)
                val startMillis = draft.date.atTime(start).atZone(zoneId).toInstant().toEpochMilli()
                val endDate = if (end.isAfter(start)) draft.date else draft.date.plusDays(1)
                val endMillis = endDate.atTime(end).atZone(zoneId).toInstant().toEpochMilli()
                put(CalendarContract.Events.DTSTART, startMillis)
                put(CalendarContract.Events.DTEND, endMillis)
                put(CalendarContract.Events.ALL_DAY, 0)
                put(CalendarContract.Events.EVENT_TIMEZONE, zoneId.id)
            }
        }

        val insertedUri = contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            ?: return@withContext null
        val newEventId = insertedUri.lastPathSegment ?: System.currentTimeMillis().toString()
        draft.toAppointment(
            id = "device-gcal-$newEventId",
            source = AppointmentSource.GOOGLE_CALENDAR,
        )
    }

    /**
     * Vraagt Android's Google Agenda SyncAdapter expliciet om direct te synchroniseren
     * zodat wijzigingen of verwijderingen op een telefoon sneller op de tablet verschijnen.
     */
    fun requestImmediateGoogleSync() {
        if (!hasReadPermission()) return
        runCatching {
            val accountNames = querySyncedGoogleCalendars()
                .map { it.accountName }
                .distinct()
            val extras = Bundle().apply {
                putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
            }
            accountNames.forEach { accountName ->
                ContentResolver.requestSync(
                    Account(accountName, GOOGLE_ACCOUNT_TYPE),
                    CalendarContract.AUTHORITY,
                    extras,
                )
            }
        }
    }

    fun registerCalendarObserver(onChanged: () -> Unit): ContentObserver? {
        if (!hasReadPermission()) return null
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                onChanged()
            }
        }
        runCatching {
            contentResolver.registerContentObserver(
                CalendarContract.Events.CONTENT_URI,
                true,
                observer,
            )
        }
        return observer
    }

    fun unregisterCalendarObserver(observer: ContentObserver?) {
        if (observer != null) {
            runCatching { contentResolver.unregisterContentObserver(observer) }
        }
    }

    private fun querySyncedGoogleCalendars(): List<SyncedGoogleCalendar> {
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.ACCOUNT_TYPE,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.OWNER_ACCOUNT,
            CalendarContract.Calendars.IS_PRIMARY,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.VISIBLE,
        )

        val selection = "${CalendarContract.Calendars.ACCOUNT_TYPE} = ? AND ${CalendarContract.Calendars.VISIBLE} = 1"
        val selectionArgs = arrayOf(GOOGLE_ACCOUNT_TYPE)

        val calendars = mutableListOf<SyncedGoogleCalendar>()
        contentResolver.query(
            CalendarContract.Calendars.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null,
        )?.use { cursor ->
            val idxId = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
            val idxAccount = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
            val idxDisplay = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
            val idxOwner = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.OWNER_ACCOUNT)
            val idxPrimary = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.IS_PRIMARY)
            val idxAccess = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idxId)
                val accountName = cursor.getString(idxAccount).orEmpty()
                val displayName = cursor.getString(idxDisplay).orEmpty()
                val ownerAccount = cursor.getString(idxOwner).orEmpty()
                val isPrimary = cursor.getInt(idxPrimary) == 1 ||
                    displayName.equals(accountName, ignoreCase = true)
                val accessLevel = cursor.getInt(idxAccess)
                val isWritable = accessLevel >= CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR

                // Sla ingebouwde feestdagen-agenda's over zodat er geen Engelse feestdagen
                // tussen de echte persoonlijke en gedeelde familieafspraken verschijnen
                if (isHolidayCalendar(ownerAccount, displayName)) continue

                calendars += SyncedGoogleCalendar(
                    id = id,
                    accountName = accountName,
                    displayName = displayName,
                    ownerAccount = ownerAccount,
                    isPrimary = isPrimary,
                    isWritable = isWritable,
                )
            }
        }
        return calendars
    }

    companion object {
        private const val GOOGLE_ACCOUNT_TYPE = "com.google"

        internal fun isHolidayCalendar(ownerAccount: String, displayName: String): Boolean {
            val ownerLower = ownerAccount.lowercase()
            val nameLower = displayName.lowercase()
            return ownerLower.contains("#holiday@group.v.calendar.google.com") ||
                nameLower.startsWith("holidays in ") ||
                nameLower.startsWith("feestdagen in ")
        }

        internal fun mapInstanceToAppointments(
            eventId: Long,
            title: String,
            beginMillis: Long,
            endMillis: Long,
            isAllDay: Boolean,
            location: String?,
            fromDate: LocalDate,
            toDateInclusive: LocalDate,
            zoneId: ZoneId,
        ): List<Appointment> {
            if (isAllDay) {
                // Android CalendarContract slaat hele-dag-events altijd op als UTC-middernacht
                val startDate = Instant.ofEpochMilli(beginMillis).atZone(ZoneOffset.UTC).toLocalDate()
                val rawEndDate = if (endMillis > beginMillis) {
                    Instant.ofEpochMilli(endMillis).atZone(ZoneOffset.UTC).toLocalDate().minusDays(1)
                } else {
                    startDate
                }
                val lastInclusive = if (rawEndDate.isBefore(startDate)) startDate else rawEndDate

                val results = mutableListOf<Appointment>()
                var cursor = if (startDate.isAfter(fromDate)) startDate else fromDate
                val limit = if (lastInclusive.isBefore(toDateInclusive)) lastInclusive else toDateInclusive
                while (!cursor.isAfter(limit)) {
                    results += Appointment.allDay(
                        id = "device-gcal-$eventId-$cursor",
                        title = title,
                        date = cursor,
                        location = location,
                        source = AppointmentSource.GOOGLE_CALENDAR,
                    )
                    cursor = cursor.plusDays(1)
                }
                return results
            }

            val startLocal = Instant.ofEpochMilli(beginMillis).atZone(zoneId).toLocalDateTime()
            val endLocal = if (endMillis > beginMillis) {
                Instant.ofEpochMilli(endMillis).atZone(zoneId).toLocalDateTime()
            } else {
                startLocal.plusHours(1)
            }

            val eventDate = startLocal.toLocalDate()
            if (eventDate.isBefore(fromDate) || eventDate.isAfter(toDateInclusive)) {
                return emptyList()
            }

            return listOf(
                Appointment.timed(
                    id = "device-gcal-$eventId-$beginMillis",
                    title = title,
                    date = eventDate,
                    startTime = startLocal.toLocalTime(),
                    endTime = endLocal.toLocalTime(),
                    location = location,
                    source = AppointmentSource.GOOGLE_CALENDAR,
                ),
            )
        }
    }
}
