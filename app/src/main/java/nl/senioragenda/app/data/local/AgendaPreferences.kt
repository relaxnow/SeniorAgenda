package nl.senioragenda.app.data.local

import android.content.Context
import nl.senioragenda.app.BuildConfig
import nl.senioragenda.app.ui.theme.DisplayThemeMode

/**
 * Slaat lokale voorkeuren op (zoals gekoppelde gedeelde agenda-ID en bevestigde herinneringen)
 * zonder gevoelige geheimen in git op te nemen.
 */
class AgendaPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var calendarId: String
        get() = prefs.getString(KEY_CALENDAR_ID, null)
            ?.takeIf { it.isNotBlank() }
            ?: BuildConfig.GOOGLE_CALENDAR_ID.ifBlank { DEFAULT_CALENDAR_ID }
        set(value) {
            prefs.edit()
                .putString(KEY_CALENDAR_ID, value.trim().ifBlank { DEFAULT_CALENDAR_ID })
                .apply()
        }

    var isGoogleCalendarConnected: Boolean
        get() = prefs.getBoolean(KEY_GOOGLE_CONNECTED, false)
        set(value) {
            prefs.edit().putBoolean(KEY_GOOGLE_CONNECTED, value).apply()
        }

    var displayThemeMode: DisplayThemeMode
        get() = DisplayThemeMode.fromStorageKey(prefs.getString(KEY_DISPLAY_THEME_MODE, null))
        set(value) {
            prefs.edit().putString(KEY_DISPLAY_THEME_MODE, value.storageKey).apply()
        }

    fun getAcknowledgedReminderIds(): Set<String> {
        return prefs.getStringSet(KEY_ACK_REMINDER_IDS, emptySet())?.toSet().orEmpty()
    }

    fun markReminderAcknowledged(appointmentId: String) {
        val updated = getAcknowledgedReminderIds().toMutableSet().apply {
            add(appointmentId)
        }
        // Houd de lijst compact zodat deze niet eindeloos groeit
        val trimmed = if (updated.size > MAX_STORED_ACK_IDS) {
            updated.toList().takeLast(MAX_STORED_ACK_IDS).toSet()
        } else {
            updated
        }
        prefs.edit().putStringSet(KEY_ACK_REMINDER_IDS, trimmed).apply()
    }

    companion object {
        private const val PREFS_NAME = "senior_agenda_prefs"
        private const val KEY_CALENDAR_ID = "google_calendar_id"
        private const val KEY_GOOGLE_CONNECTED = "google_calendar_connected"
        private const val KEY_DISPLAY_THEME_MODE = "display_theme_mode"
        private const val KEY_ACK_REMINDER_IDS = "ack_reminder_ids"
        private const val DEFAULT_CALENDAR_ID = "primary"
        private const val MAX_STORED_ACK_IDS = 120
    }
}
