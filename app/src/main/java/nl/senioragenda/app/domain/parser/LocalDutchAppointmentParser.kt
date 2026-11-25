package nl.senioragenda.app.domain.parser

import nl.senioragenda.app.domain.model.Appointment
import nl.senioragenda.app.domain.model.ParsedAppointmentDraft
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Month

/**
 * Lokale deterministische Nederlandse parser voor ingesproken of getypte afspraken.
 * Werkt zowel offline als terugvaloptie wanneer er nog geen Gemini API-sleutel is ingesteld.
 *
 * Voorbeelden die worden herkend:
 * - "Morgen van 14:00 tot 15:00 huisarts"
 * - "Vrijdag om half drie tandarts" -> standaard 1 uur (14:30 - 15:30)
 * - "Zaterdag hele dag hapjes komen aan"
 * - "Overmorgen om 10 uur 15 tot 11 uur fysiotherapie"
 */
object LocalDutchAppointmentParser {

    private val WEEKDAY_MAP = mapOf(
        "maandag" to DayOfWeek.MONDAY,
        "dinsdag" to DayOfWeek.TUESDAY,
        "woensdag" to DayOfWeek.WEDNESDAY,
        "donderdag" to DayOfWeek.THURSDAY,
        "vrijdag" to DayOfWeek.FRIDAY,
        "zaterdag" to DayOfWeek.SATURDAY,
        "zondag" to DayOfWeek.SUNDAY,
    )

    private val MONTH_MAP = mapOf(
        "januari" to Month.JANUARY,
        "jan" to Month.JANUARY,
        "februari" to Month.FEBRUARY,
        "feb" to Month.FEBRUARY,
        "maart" to Month.MARCH,
        "mrt" to Month.MARCH,
        "april" to Month.APRIL,
        "apr" to Month.APRIL,
        "mei" to Month.MAY,
        "juni" to Month.JUNE,
        "jun" to Month.JUNE,
        "juli" to Month.JULY,
        "jul" to Month.JULY,
        "augustus" to Month.AUGUST,
        "aug" to Month.AUGUST,
        "september" to Month.SEPTEMBER,
        "sep" to Month.SEPTEMBER,
        "oktober" to Month.OCTOBER,
        "okt" to Month.OCTOBER,
        "november" to Month.NOVEMBER,
        "nov" to Month.NOVEMBER,
        "december" to Month.DECEMBER,
        "dec" to Month.DECEMBER,
    )

    private val NUMBER_WORDS = mapOf(
        "een" to 1,
        "één" to 1,
        "twee" to 2,
        "drie" to 3,
        "vier" to 4,
        "vijf" to 5,
        "zes" to 6,
        "zeven" to 7,
        "acht" to 8,
        "negen" to 9,
        "tien" to 10,
        "elf" to 11,
        "twaalf" to 12,
    )

    private val RANGE_REGEX = Regex(
        pattern = """(?i)\b(?:van\s+)?(\d{1,2})(?:[:.](\d{2})|\s*uur(?:\s*(\d{1,2}))?)\s*(?:tot|-|–)\s*(\d{1,2})(?:[:.](\d{2})|\s*uur(?:\s*(\d{1,2}))?)\b""",
    )

    private val SINGLE_COLON_TIME_REGEX = Regex(
        pattern = """(?i)\b(?:om\s+)?(\d{1,2})[:.](\d{2})(?:\s*uur)?\b""",
    )

    private val SINGLE_UUR_REGEX = Regex(
        pattern = """(?i)\b(?:om\s+)?(\d{1,2})\s*uur(?:\s*(\d{1,2}))?\b""",
    )

    private val HALF_TIME_REGEX = Regex(
        pattern = """(?i)\b(?:om\s+)?half\s+(\d{1,2}|een|één|twee|drie|vier|vijf|zes|zeven|acht|negen|tien|elf|twaalf)\b""",
    )

    private val KWART_OVER_REGEX = Regex(
        pattern = """(?i)\b(?:om\s+)?kwart\s+over\s+(\d{1,2}|een|één|twee|drie|vier|vijf|zes|zeven|acht|negen|tien|elf|twaalf)\b""",
    )

    private val KWART_VOOR_REGEX = Regex(
        pattern = """(?i)\b(?:om\s+)?kwart\s+voor\s+(\d{1,2}|een|één|twee|drie|vier|vijf|zes|zeven|acht|negen|tien|elf|twaalf)\b""",
    )

    private val EXPLICIT_DATE_REGEX = Regex(
        pattern = """(?i)\b(\d{1,2})\s+(januari|jan|februari|feb|maart|mrt|april|apr|mei|juni|jun|juli|jul|augustus|aug|september|sep|oktober|okt|november|nov|december|dec)\b""",
    )

    private val ALL_DAY_REGEX = Regex(
        pattern = """(?i)\b(?:de\s+)?(?:hele|gehele)\s+dag\b""",
    )

    fun parse(
        input: String,
        referenceNow: LocalDateTime,
    ): ParsedAppointmentDraft? {
        val raw = input.trim()
        if (raw.isBlank()) return null

        var working = raw
        val lower = raw.lowercase()
        val isMorningHint = lower.contains("ochtend") || lower.contains("'s morgens") || lower.contains("s morgens")
        val isEveningHint = lower.contains("avond") || lower.contains("'s avonds") || lower.contains("s avonds")

        // 1. Datum bepalen
        var resolvedDate: LocalDate = referenceNow.toLocalDate()
        when {
            Regex("""(?i)\bovermorgen\b""").containsMatchIn(working) -> {
                resolvedDate = referenceNow.toLocalDate().plusDays(2)
                working = working.replace(Regex("""(?i)\bovermorgen\b"""), " ")
            }

            Regex("""(?i)\bmorgen\b""").containsMatchIn(working) -> {
                resolvedDate = referenceNow.toLocalDate().plusDays(1)
                working = working.replace(Regex("""(?i)\bmorgen\b"""), " ")
            }

            Regex("""(?i)\bvandaag\b""").containsMatchIn(working) -> {
                resolvedDate = referenceNow.toLocalDate()
                working = working.replace(Regex("""(?i)\bvandaag\b"""), " ")
            }

            else -> {
                val explicitDateMatch = EXPLICIT_DATE_REGEX.find(working)
                if (explicitDateMatch != null) {
                    val day = explicitDateMatch.groupValues[1].toIntOrNull()
                    val month = MONTH_MAP[explicitDateMatch.groupValues[2].lowercase()]
                    if (day != null && month != null) {
                        val today = referenceNow.toLocalDate()
                        val candidate = runCatching { LocalDate.of(today.year, month, day) }.getOrNull()
                        if (candidate != null) {
                            resolvedDate = if (candidate.isBefore(today.minusDays(1))) {
                                candidate.plusYears(1)
                            } else {
                                candidate
                            }
                            working = working.replaceRange(explicitDateMatch.range, " ")
                        }
                    }
                } else {
                    for ((dayWord, targetDow) in WEEKDAY_MAP) {
                        val dayRegex = Regex("""(?i)\b(?:aanstaande\s+|komende\s+|op\s+)?$dayWord\b""")
                        val match = dayRegex.find(working)
                        if (match != null) {
                            resolvedDate = nextDateForDayOfWeek(referenceNow.toLocalDate(), targetDow)
                            working = working.replaceRange(match.range, " ")
                            break
                        }
                    }
                }
            }
        }

        // 2. Hele dag of tijdsgebonden bepalen
        val isAllDay = ALL_DAY_REGEX.containsMatchIn(working)
        if (isAllDay) {
            working = working.replace(ALL_DAY_REGEX, " ")
            val title = cleanTitle(working)
            return ParsedAppointmentDraft(
                title = title,
                date = resolvedDate,
                isAllDay = true,
                startTime = null,
                endTime = null,
                parsedByGemini = false,
            )
        }

        var startTime: LocalTime? = null
        var endTime: LocalTime? = null

        val rangeMatch = RANGE_REGEX.find(working)
        if (rangeMatch != null) {
            val h1 = rangeMatch.groupValues[1].toIntOrNull()
            val m1 = rangeMatch.groupValues[2].ifBlank { rangeMatch.groupValues[3] }.toIntOrNull() ?: 0
            val h2 = rangeMatch.groupValues[4].toIntOrNull()
            val m2 = rangeMatch.groupValues[5].ifBlank { rangeMatch.groupValues[6] }.toIntOrNull() ?: 0
            if (h1 != null && h2 != null && h1 in 0..23 && h2 in 0..23 && m1 in 0..59 && m2 in 0..59) {
                startTime = LocalTime.of(h1, m1)
                endTime = LocalTime.of(h2, m2)
                working = working.replaceRange(rangeMatch.range, " ")
            }
        }

        if (startTime == null) {
            val colonMatch = SINGLE_COLON_TIME_REGEX.find(working)
            if (colonMatch != null) {
                val h = colonMatch.groupValues[1].toIntOrNull()
                val m = colonMatch.groupValues[2].toIntOrNull()
                if (h != null && m != null && h in 0..23 && m in 0..59) {
                    startTime = LocalTime.of(h, m)
                    working = working.replaceRange(colonMatch.range, " ")
                }
            }
        }

        if (startTime == null) {
            val halfMatch = HALF_TIME_REGEX.find(working)
            if (halfMatch != null) {
                val spokenHour = parseSpokenHour(halfMatch.groupValues[1])
                if (spokenHour != null) {
                    val baseHour = adjustDutch12Hour(spokenHour, isMorningHint, isEveningHint)
                    val actualHour = (baseHour - 1 + 24) % 24
                    startTime = LocalTime.of(actualHour, 30)
                    working = working.replaceRange(halfMatch.range, " ")
                }
            }
        }

        if (startTime == null) {
            val kwartOverMatch = KWART_OVER_REGEX.find(working)
            if (kwartOverMatch != null) {
                val spokenHour = parseSpokenHour(kwartOverMatch.groupValues[1])
                if (spokenHour != null) {
                    val actualHour = adjustDutch12Hour(spokenHour, isMorningHint, isEveningHint)
                    startTime = LocalTime.of(actualHour, 15)
                    working = working.replaceRange(kwartOverMatch.range, " ")
                }
            }
        }

        if (startTime == null) {
            val kwartVoorMatch = KWART_VOOR_REGEX.find(working)
            if (kwartVoorMatch != null) {
                val spokenHour = parseSpokenHour(kwartVoorMatch.groupValues[1])
                if (spokenHour != null) {
                    val baseHour = adjustDutch12Hour(spokenHour, isMorningHint, isEveningHint)
                    val actualHour = (baseHour - 1 + 24) % 24
                    startTime = LocalTime.of(actualHour, 45)
                    working = working.replaceRange(kwartVoorMatch.range, " ")
                }
            }
        }

        if (startTime == null) {
            val uurMatch = SINGLE_UUR_REGEX.find(working)
            if (uurMatch != null) {
                val h = uurMatch.groupValues[1].toIntOrNull()
                val m = uurMatch.groupValues[2].toIntOrNull() ?: 0
                if (h != null && h in 0..23 && m in 0..59) {
                    val adjustedH = if (h in 1..7 && !isMorningHint) h + 12 else h
                    startTime = LocalTime.of(adjustedH, m)
                    working = working.replaceRange(uurMatch.range, " ")
                }
            }
        }

        // Als er geen tijd is genoemd, maak er standaard een hele-dag-afspraak van
        if (startTime == null) {
            return ParsedAppointmentDraft(
                title = cleanTitle(working),
                date = resolvedDate,
                isAllDay = true,
                startTime = null,
                endTime = null,
                parsedByGemini = false,
            )
        }

        // Standaardduur 1 uur als geen eindtijd is genoemd
        val resolvedEndTime = endTime?.takeIf { it != startTime }
            ?: Appointment.defaultEndTime(startTime)

        return ParsedAppointmentDraft(
            title = cleanTitle(working),
            date = resolvedDate,
            isAllDay = false,
            startTime = startTime,
            endTime = resolvedEndTime,
            parsedByGemini = false,
        )
    }

    private fun parseSpokenHour(token: String): Int? {
        val cleaned = token.lowercase().trim()
        return cleaned.toIntOrNull()?.takeIf { it in 1..23 } ?: NUMBER_WORDS[cleaned]
    }

    private fun adjustDutch12Hour(
        hour: Int,
        isMorningHint: Boolean,
        isEveningHint: Boolean,
    ): Int {
        return when {
            hour >= 13 -> hour
            isEveningHint && hour in 1..11 -> hour + 12
            !isMorningHint && hour in 1..7 -> hour + 12
            else -> hour
        }
    }

    private fun nextDateForDayOfWeek(today: LocalDate, target: DayOfWeek): LocalDate {
        var candidate = today
        while (candidate.dayOfWeek != target) {
            candidate = candidate.plusDays(1)
        }
        return candidate
    }

    private fun cleanTitle(raw: String): String {
        val cleaned = raw
            .replace(Regex("""(?i)\b(afspraak|toevoegen|zet\s+in\s+de\s+agenda|in\s+de\s+agenda|op\s+de\s+agenda|naar\s+de|naar\s+het|bij\s+de|in\s+de\s+ochtend|in\s+de\s+middag|in\s+de\s+avond|'s\s+morgens|'s\s+middags|'s\s+avonds)\b"""), " ")
            .replace(Regex("""^[\s,.:;\-]+|[\s,.:;\-]+$"""), "")
            .replace(Regex("""\s{2,}"""), " ")
            .trim()

        val finalTitle = cleaned.ifBlank { "Nieuwe afspraak" }
        return finalTitle.replaceFirstChar { ch ->
            if (ch.isLowerCase()) ch.titlecase() else ch.toString()
        }
    }
}
