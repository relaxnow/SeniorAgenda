package nl.senioragenda.app.reminder

import nl.senioragenda.app.domain.model.Appointment
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class ActiveReminder(
    val appointment: Appointment,
    val triggeredSlotIndex: Int,
    val isManualTest: Boolean = false,
)

data class ReminderEvaluation(
    val activeAppointment: Appointment?,
    val currentSlotIndex: Int?,
    val shouldSpeakNow: Boolean,
)

/**
 * Zuivere planningslogica voor tijdige herinneringen:
 * - Start exact 30 minuten vóór de begintijd van een tijdsgebonden afspraak.
 * - Herhaalt elke 5 minuten (T-30, T-25, T-20, T-15, T-10, T-5) totdat de afspraak begint
 *   of de gebruiker op "Begrepen" tikt.
 */
object ReminderPolicy {

    const val LEAD_TIME_MINUTES: Long = 30L
    const val REPEAT_INTERVAL_MINUTES: Long = 5L
    const val TOTAL_REPEAT_SLOTS: Int = 6 // 0=T-30, 1=T-25, 2=T-20, 3=T-15, 4=T-10, 5=T-5

    /**
     * Geeft alle herinneringsmomenten terug voor één tijdsgebonden afspraak.
     * Hele-dag-items genereren geen 30-minuten-vooraf alarm.
     */
    fun reminderTriggersFor(appointment: Appointment): List<LocalDateTime> {
        if (appointment.isAllDay) return emptyList()
        val start = appointment.startDateTime ?: return emptyList()
        val firstTrigger = start.minusMinutes(LEAD_TIME_MINUTES)
        return (0 until TOTAL_REPEAT_SLOTS).map { slot ->
            firstTrigger.plusMinutes(slot * REPEAT_INTERVAL_MINUTES)
        }
    }

    /**
     * Geeft het eerstvolgende alarmtijdstip ná [now] voor een reeks afspraken
     * (waarbij reeds bevestigde afspraken worden overgeslagen).
     */
    fun nextScheduledTrigger(
        appointments: List<Appointment>,
        now: LocalDateTime,
        acknowledgedIds: Set<String>,
    ): Pair<Appointment, LocalDateTime>? {
        return appointments
            .asSequence()
            .filter { !it.isAllDay && it.id !in acknowledgedIds }
            .mapNotNull { appointment ->
                val nextTrigger = reminderTriggersFor(appointment)
                    .firstOrNull { trigger -> trigger.isAfter(now) }
                nextTrigger?.let { appointment to it }
            }
            .minWithOrNull(Comparator { first, second -> first.second.compareTo(second.second) })
    }

    /**
     * Bepaalt of er op tijdstip [now] een herinnering actief moet zijn en of de
     * Nederlandse spraakmelding opnieuw moet worden uitgesproken.
     */
    fun evaluate(
        appointments: List<Appointment>,
        now: LocalDateTime,
        acknowledgedIds: Set<String>,
        lastSpokenSlotById: Map<String, Int>,
        currentlyDisplayedId: String? = null,
    ): ReminderEvaluation {
        val candidates = appointments
            .asSequence()
            .filter { !it.isAllDay && it.id !in acknowledgedIds }
            .mapNotNull { appointment ->
                val start = appointment.startDateTime ?: return@mapNotNull null
                val windowStart = start.minusMinutes(LEAD_TIME_MINUTES)
                val inActiveRepeatWindow = !now.isBefore(windowStart) && now.isBefore(start)
                val stillDisplayedUntilUnderstood = appointment.id == currentlyDisplayedId &&
                    !now.isBefore(windowStart)

                when {
                    inActiveRepeatWindow -> {
                        val elapsedMinutes = ChronoUnit.MINUTES.between(windowStart, now)
                            .coerceAtLeast(0L)
                        val slot = (elapsedMinutes / REPEAT_INTERVAL_MINUTES)
                            .toInt()
                            .coerceIn(0, TOTAL_REPEAT_SLOTS - 1)
                        Candidate(appointment = appointment, slotIndex = slot, repeatActive = true)
                    }

                    stillDisplayedUntilUnderstood -> {
                        // Afspraak is inmiddels begonnen: niet meer elke 5 minuten herhalen,
                        // maar het scherm blijft staan totdat op "Begrepen" is getikt.
                        Candidate(
                            appointment = appointment,
                            slotIndex = TOTAL_REPEAT_SLOTS,
                            repeatActive = false,
                        )
                    }

                    else -> null
                }
            }
            .sortedWith(
                compareBy<Candidate>(
                    { it.appointment.id != currentlyDisplayedId },
                    { it.appointment.startDateTime },
                ),
            )
            .toList()

        val winner = candidates.firstOrNull()
            ?: return ReminderEvaluation(
                activeAppointment = null,
                currentSlotIndex = null,
                shouldSpeakNow = false,
            )

        val previousSlot = lastSpokenSlotById[winner.appointment.id]
        val shouldSpeak = winner.repeatActive &&
            (previousSlot == null || winner.slotIndex > previousSlot)

        return ReminderEvaluation(
            activeAppointment = winner.appointment,
            currentSlotIndex = winner.slotIndex,
            shouldSpeakNow = shouldSpeak,
        )
    }

    private data class Candidate(
        val appointment: Appointment,
        val slotIndex: Int,
        val repeatActive: Boolean,
    )
}
