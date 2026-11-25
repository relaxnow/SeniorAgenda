package nl.senioragenda.app.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import nl.senioragenda.app.domain.model.Appointment
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Plant systeemalarmen via [AlarmManager] zodat ook tijdens nachtstand of doze-modus
 * een tijdige herinnering (30 minuten vooraf + elke 5 minuten) het scherm van de Tab S9 FE
 * direct kan wekken.
 */
class ReminderAlarmScheduler(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleNextReminder(
        appointments: List<Appointment>,
        now: LocalDateTime,
        acknowledgedIds: Set<String>,
        zoneId: ZoneId = ZoneId.systemDefault(),
    ) {
        val next = ReminderPolicy.nextScheduledTrigger(
            appointments = appointments,
            now = now,
            acknowledgedIds = acknowledgedIds,
        )

        if (next == null) {
            cancelScheduledReminder()
            return
        }

        val (appointment, triggerDateTime) = next
        val triggerAtMillis = triggerDateTime.atZone(zoneId).toInstant().toEpochMilli()

        val pendingIntent = buildPendingIntent(appointment.id)
        val canUseExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarmManager.canScheduleExactAlarms()

        try {
            if (canUseExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
    }

    fun cancelScheduledReminder() {
        alarmManager.cancel(buildPendingIntent(appointmentId = null))
    }

    private fun buildPendingIntent(appointmentId: String?): PendingIntent {
        val intent = Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_REMINDER
            if (appointmentId != null) {
                putExtra(EXTRA_APPOINTMENT_ID, appointmentId)
            }
        }
        return PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE_NEXT_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    companion object {
        const val ACTION_TRIGGER_REMINDER = "nl.senioragenda.app.action.TRIGGER_REMINDER"
        const val EXTRA_APPOINTMENT_ID = "extra_appointment_id"
        private const val REQUEST_CODE_NEXT_REMINDER = 3005
    }
}
