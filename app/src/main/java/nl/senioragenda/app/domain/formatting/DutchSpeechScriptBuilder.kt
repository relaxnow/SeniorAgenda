package nl.senioragenda.app.domain.formatting

import nl.senioragenda.app.domain.model.AgendaSnapshot
import nl.senioragenda.app.domain.model.Appointment
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Stelt natuurlijke, rustige Nederlandse teksten samen voor spraakuitvoer (TTS).
 */
object DutchSpeechScriptBuilder {

    fun buildDailyBriefing(
        snapshot: AgendaSnapshot,
        now: LocalDateTime,
    ): String {
        val parts = mutableListOf<String>()

        parts += "${greetingFor(now.toLocalTime())}."
        parts += "Het is ${DutchDateTimeFormatter.formatSpokenDate(now.toLocalDate())}, ${DutchDateTimeFormatter.formatSpokenTime(now.toLocalTime())}."

        val today = snapshot.today
        if (today.isEmpty) {
            parts += "U heeft vandaag geen afspraken. Het is een rustige dag."
        } else {
            val count = today.appointments.size
            parts += if (count == 1) {
                "Vandaag heeft u 1 afspraak."
            } else {
                "Vandaag heeft u $count afspraken."
            }
            today.allDayItems.forEach { item ->
                parts += "De hele dag geldt: ${item.title}."
            }
            today.timedItems.forEach { item ->
                val start = DutchDateTimeFormatter.formatSpokenTime(requireNotNull(item.startTime))
                val end = DutchDateTimeFormatter.formatSpokenTime(requireNotNull(item.endTime))
                parts += "Van $start tot $end heeft u: ${item.title}."
            }
        }

        val tomorrow = snapshot.tomorrow
        val tomorrowDate = DutchDateTimeFormatter.formatSpokenDate(tomorrow.date)
        if (tomorrow.isEmpty) {
            parts += "Morgen, $tomorrowDate, heeft u geen afspraken."
        } else {
            val count = tomorrow.appointments.size
            parts += if (count == 1) {
                "Morgen, $tomorrowDate, heeft u 1 afspraak."
            } else {
                "Morgen, $tomorrowDate, heeft u $count afspraken."
            }
            tomorrow.allDayItems.forEach { item ->
                parts += "De hele dag: ${item.title}."
            }
            tomorrow.timedItems.forEach { item ->
                val start = DutchDateTimeFormatter.formatSpokenTime(requireNotNull(item.startTime))
                val end = DutchDateTimeFormatter.formatSpokenTime(requireNotNull(item.endTime))
                parts += "Van $start tot $end: ${item.title}."
            }
        }

        return parts.joinToString(" ")
    }

    fun buildReminderAnnouncement(
        appointment: Appointment,
        now: LocalDateTime,
    ): String {
        val start = requireNotNull(appointment.startTime)
        val end = requireNotNull(appointment.endTime)
        val startDateTime = requireNotNull(appointment.startDateTime)
        val minutesRemaining = ChronoUnit.MINUTES.between(now, startDateTime).coerceAtLeast(0)

        val timingPhrase = when {
            minutesRemaining <= 0L -> "Uw afspraak begint nu."
            minutesRemaining == 1L -> "Over 1 minuut begint uw afspraak."
            else -> "Over $minutesRemaining minuten begint uw afspraak."
        }

        val spokenStart = DutchDateTimeFormatter.formatSpokenTime(start)
        val spokenEnd = DutchDateTimeFormatter.formatSpokenTime(end)

        return "Let op. Herinnering voor vandaag. $timingPhrase Van $spokenStart tot $spokenEnd heeft u: ${appointment.title}. Tik op Begrepen om deze melding te sluiten."
    }

    fun formatCountdownBadge(
        appointment: Appointment,
        now: LocalDateTime,
    ): String {
        val startDateTime = appointment.startDateTime ?: return "Vandaag"
        val minutesRemaining = ChronoUnit.MINUTES.between(now, startDateTime)
        return when {
            minutesRemaining <= 0L -> "Begint nu"
            minutesRemaining == 1L -> "Over 1 minuut"
            else -> "Over $minutesRemaining minuten"
        }
    }

    private fun greetingFor(time: LocalTime): String = when (time.hour) {
        in 5..11 -> "Goedemorgen"
        in 12..17 -> "Goedemiddag"
        else -> "Goedenavond"
    }
}
