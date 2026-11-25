package nl.senioragenda.app.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import nl.senioragenda.app.MainActivity

/**
 * Ontvangt systeemalarmen van [ReminderAlarmScheduler], wekt het scherm van de tablet
 * (ook tijdens nachtstand) en opent [MainActivity] met prioriteit voor de herinneringsoverlay.
 */
class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != ReminderAlarmScheduler.ACTION_TRIGGER_REMINDER) return

        val appointmentId = intent.getStringExtra(ReminderAlarmScheduler.EXTRA_APPOINTMENT_ID)
        wakeDeviceBriefly(context)

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_SINGLE_TOP or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            putExtra(ReminderAlarmScheduler.EXTRA_APPOINTMENT_ID, appointmentId)
        }
        context.startActivity(launchIntent)
    }

    private fun wakeDeviceBriefly(context: Context) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            ?: return
        @Suppress("DEPRECATION")
        val wakeLock = powerManager.newWakeLock(
            PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "SeniorAgenda:ReminderWakeLock",
        )
        runCatching {
            wakeLock.acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    companion object {
        private const val WAKE_LOCK_TIMEOUT_MS = 10_000L
    }
}
