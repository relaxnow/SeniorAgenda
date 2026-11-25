package nl.senioragenda.app.nightmode

import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Nachtstandbeleid speciaal afgestemd op het TFT LCD-scherm van de Samsung Galaxy Tab S9 FE:
 * - Tussen 23:30 en 05:30 dimt de achtergrondverlichting naar het minimum (0.01f) met een
 *   zeer donker rustscherm, zodat de kamer donker blijft en warmte/energieverbruik laag blijven.
 * - Tikken op het scherm wekt het dashboard tijdelijk voor 30 seconden.
 * - Om 05:30 keert het dashboard automatisch terug naar de normale dagstand.
 * - Elke actieve herinnering (inclusief 30-minuten vooraf en 5-minuten herhalingen) krijgt
 *   direct voorrang en wekt het scherm.
 */
sealed interface NightModeState {
    val targetScreenBrightness: Float

    data object Daytime : NightModeState {
        override val targetScreenBrightness: Float = NightModePolicy.DAYTIME_BRIGHTNESS_SYSTEM_DEFAULT
    }

    data object Sleeping : NightModeState {
        override val targetScreenBrightness: Float = NightModePolicy.LCD_MINIMUM_BRIGHTNESS
    }

    data class TemporaryWake(
        val secondsRemaining: Long,
    ) : NightModeState {
        override val targetScreenBrightness: Float = NightModePolicy.TEMPORARY_WAKE_BRIGHTNESS
    }

    data object ReminderWakeOverride : NightModeState {
        override val targetScreenBrightness: Float = NightModePolicy.REMINDER_OVERRIDE_BRIGHTNESS
    }
}

object NightModePolicy {

    val NIGHT_START: LocalTime = LocalTime.of(23, 30)
    val NIGHT_END: LocalTime = LocalTime.of(5, 30)

    const val TEMPORARY_WAKE_SECONDS: Long = 30L

    /** -1f = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE (systeem/daghelderheid) */
    const val DAYTIME_BRIGHTNESS_SYSTEM_DEFAULT: Float = -1f

    /** Minimale helderheid voor TFT LCD zodat achtergrondverlichting nauwelijks licht afgeeft */
    const val LCD_MINIMUM_BRIGHTNESS: Float = 0.01f

    /** Comfortabele, niet-verblindende helderheid bij even kort kijken in de nacht */
    const val TEMPORARY_WAKE_BRIGHTNESS: Float = 0.55f

    /** Goed leesbare helderheid wanneer een herinnering tijdens nachtstand afgaat */
    const val REMINDER_OVERRIDE_BRIGHTNESS: Float = 0.85f

    fun isInNightSchedule(time: LocalTime): Boolean {
        return !time.isBefore(NIGHT_START) || time.isBefore(NIGHT_END)
    }

    fun resolve(
        now: LocalDateTime,
        hasActiveReminder: Boolean,
        temporaryWakeUntil: LocalDateTime?,
        manualNightPreview: Boolean = false,
    ): NightModeState {
        if (hasActiveReminder) {
            return NightModeState.ReminderWakeOverride
        }

        val nightActive = manualNightPreview || isInNightSchedule(now.toLocalTime())
        if (!nightActive) {
            return NightModeState.Daytime
        }

        if (temporaryWakeUntil != null && now.isBefore(temporaryWakeUntil)) {
            val remaining = ChronoUnit.SECONDS.between(now, temporaryWakeUntil).coerceAtLeast(1L)
            return NightModeState.TemporaryWake(secondsRemaining = remaining)
        }

        return NightModeState.Sleeping
    }
}
