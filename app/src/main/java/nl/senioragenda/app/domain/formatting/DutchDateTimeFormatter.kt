package nl.senioragenda.app.domain.formatting

import nl.senioragenda.app.domain.model.Appointment
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Strikte Nederlandse (`nl-NL`) datum- en tijdopmaak met 24-uurs notatie.
 * Gebruikt expliciete Nederlandse tabellen zodat de weergave op elk Android-apparaat
 * onafhankelijk van de systeemtaal 100% Nederlands blijft.
 */
object DutchDateTimeFormatter {

    val DUTCH_LOCALE: Locale = Locale.forLanguageTag("nl-NL")

    private val TIME_24H: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm", DUTCH_LOCALE)

    private val FULL_DAY_NAMES: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.MONDAY to "maandag",
        DayOfWeek.TUESDAY to "dinsdag",
        DayOfWeek.WEDNESDAY to "woensdag",
        DayOfWeek.THURSDAY to "donderdag",
        DayOfWeek.FRIDAY to "vrijdag",
        DayOfWeek.SATURDAY to "zaterdag",
        DayOfWeek.SUNDAY to "zondag",
    )

    private val SHORT_DAY_NAMES: Map<DayOfWeek, String> = mapOf(
        DayOfWeek.MONDAY to "Ma",
        DayOfWeek.TUESDAY to "Di",
        DayOfWeek.WEDNESDAY to "Wo",
        DayOfWeek.THURSDAY to "Do",
        DayOfWeek.FRIDAY to "Vr",
        DayOfWeek.SATURDAY to "Za",
        DayOfWeek.SUNDAY to "Zo",
    )

    private val FULL_MONTH_NAMES: Map<Month, String> = mapOf(
        Month.JANUARY to "januari",
        Month.FEBRUARY to "februari",
        Month.MARCH to "maart",
        Month.APRIL to "april",
        Month.MAY to "mei",
        Month.JUNE to "juni",
        Month.JULY to "juli",
        Month.AUGUST to "augustus",
        Month.SEPTEMBER to "september",
        Month.OCTOBER to "oktober",
        Month.NOVEMBER to "november",
        Month.DECEMBER to "december",
    )

    private val SHORT_MONTH_NAMES: Map<Month, String> = mapOf(
        Month.JANUARY to "jan",
        Month.FEBRUARY to "feb",
        Month.MARCH to "mrt",
        Month.APRIL to "apr",
        Month.MAY to "mei",
        Month.JUNE to "jun",
        Month.JULY to "jul",
        Month.AUGUST to "aug",
        Month.SEPTEMBER to "sep",
        Month.OCTOBER to "okt",
        Month.NOVEMBER to "nov",
        Month.DECEMBER to "dec",
    )

    /** Bijvoorbeeld: "DONDERDAG 26 NOVEMBER" */
    fun formatTopBarDate(date: LocalDate): String {
        val day = FULL_DAY_NAMES.getValue(date.dayOfWeek)
        val month = FULL_MONTH_NAMES.getValue(date.month)
        return "$day ${date.dayOfMonth} $month".uppercase(DUTCH_LOCALE)
    }

    /** Bijvoorbeeld: "MORGEN - VRIJDAG 27 NOVEMBER" */
    fun formatTomorrowHeader(date: LocalDate): String {
        return "MORGEN - ${formatTopBarDate(date)}"
    }

    /** Bijvoorbeeld: "donderdag 26 november" */
    fun formatSpokenDate(date: LocalDate): String {
        val day = FULL_DAY_NAMES.getValue(date.dayOfWeek)
        val month = FULL_MONTH_NAMES.getValue(date.month)
        return "$day ${date.dayOfMonth} $month"
    }

    /** Bijvoorbeeld: "Za 28 nov" */
    fun formatCompactDay(date: LocalDate): String {
        val shortDay = SHORT_DAY_NAMES.getValue(date.dayOfWeek)
        val shortMonth = SHORT_MONTH_NAMES.getValue(date.month)
        return "$shortDay ${date.dayOfMonth} $shortMonth"
    }

    /** Bijvoorbeeld: "10:00" */
    fun formatTime(time: LocalTime): String = time.format(TIME_24H)

    /** Bijvoorbeeld: "10:15 - 11:00" */
    fun formatTimeRange(startTime: LocalTime, endTime: LocalTime): String {
        return "${formatTime(startTime)} - ${formatTime(endTime)}"
    }

    /** Bijvoorbeeld: "HELE DAG" of "10:15 - 11:00" */
    fun formatAppointmentTimeLabel(appointment: Appointment): String {
        return if (appointment.isAllDay) {
            "HELE DAG"
        } else {
            formatTimeRange(
                requireNotNull(appointment.startTime),
                requireNotNull(appointment.endTime),
            )
        }
    }

    /** Natuurlijke Nederlandse uitspraak voor tijden, bijvoorbeeld "10 uur 15" of "14 uur". */
    fun formatSpokenTime(time: LocalTime): String {
        return if (time.minute == 0) {
            "${time.hour} uur"
        } else {
            val minuteText = time.minute.toString().padStart(2, '0')
            "${time.hour} uur $minuteText"
        }
    }
}
