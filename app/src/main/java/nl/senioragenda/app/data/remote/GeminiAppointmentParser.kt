package nl.senioragenda.app.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import nl.senioragenda.app.BuildConfig
import nl.senioragenda.app.domain.formatting.DutchDateTimeFormatter
import nl.senioragenda.app.domain.model.Appointment
import nl.senioragenda.app.domain.model.ParsedAppointmentDraft
import nl.senioragenda.app.domain.parser.LocalDutchAppointmentParser
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Verwerkt natuurlijke Nederlandse zinnen met de Gemini API naar een gestructureerde afspraak
 * (datum, hele dag of begin- en eindtijd met standaardduur 1 uur, en duidelijke Nederlandse titel).
 *
 * Wanneer er nog geen `GEMINI_API_KEY` is geconfigureerd of bij netwerkproblemen,
 * valt deze klasse naadloos terug op [LocalDutchAppointmentParser].
 */
class GeminiAppointmentParser(
    private val apiKey: String = BuildConfig.GEMINI_API_KEY,
    private val httpClient: OkHttpClient = GoogleCalendarApiClient.defaultHttpClient(),
) {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    val isGeminiConfigured: Boolean
        get() = apiKey.isNotBlank()

    suspend fun parseDutchInput(
        dutchText: String,
        referenceNow: LocalDateTime,
    ): ParsedAppointmentDraft? {
        val trimmed = dutchText.trim()
        if (trimmed.isBlank()) return null

        if (apiKey.isNotBlank()) {
            val geminiResult = runCatching {
                callGemini(trimmed, referenceNow)
            }.getOrNull()
            if (geminiResult != null) {
                return geminiResult
            }
        }

        return LocalDutchAppointmentParser.parse(trimmed, referenceNow)
    }

    private suspend fun callGemini(
        dutchText: String,
        referenceNow: LocalDateTime,
    ): ParsedAppointmentDraft? = withContext(Dispatchers.IO) {
        val todaySpoken = DutchDateTimeFormatter.formatSpokenDate(referenceNow.toLocalDate())
        val prompt = """
            Je bent een Nederlandse agenda-assistent voor een oudere gebruiker.
            Zet de volgende Nederlandse zin om naar exact één JSON-object zonder extra uitleg.

            Huidige datum en tijd:
            - Datum: ${referenceNow.toLocalDate()} ($todaySpoken)
            - Tijd: ${DutchDateTimeFormatter.formatTime(referenceNow.toLocalTime())}

            Regels:
            1. Gebruik 24-uurs tijdnotatie "HH:mm".
            2. Als de gebruiker "hele dag" of geen specifieke tijd noemt voor een daggebeurtenis, zet "isAllDay": true en laat "startTime" en "endTime" null.
            3. Als er wél een begintijd is maar géén eindtijd, bereken dan automatisch een eindtijd van exact 1 uur later.
            4. Maak "title" kort, duidelijk en natuurlijk Nederlands met een hoofdletter.
            5. "date" moet het formaat "YYYY-MM-DD" hebben.

            Antwoord strikt met JSON met deze velden:
            {"title":"...","date":"YYYY-MM-DD","isAllDay":false,"startTime":"14:00","endTime":"15:00"}

            Invoer van gebruiker:
            "$dutchText"
        """.trimIndent()

        val payload = GeminiGenerateRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(GeminiPart(text = prompt)),
                ),
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.1,
            ),
        )

        val request = Request.Builder()
            .url("$GEMINI_ENDPOINT?key=$apiKey")
            .post(json.encodeToString(payload).toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@withContext null
            val body = response.body?.string().orEmpty()
            val decoded = json.decodeFromString<GeminiGenerateResponse>(body)
            val rawJson = decoded.candidates
                .firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: return@withContext null

            parseStructuredJson(rawJson)
        }
    }

    internal fun parseStructuredJson(rawJson: String): ParsedAppointmentDraft? {
        val cleaned = rawJson
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        val dto = runCatching {
            json.decodeFromString<GeminiAppointmentDto>(cleaned)
        }.getOrNull() ?: return null

        val date = runCatching { LocalDate.parse(dto.date) }.getOrNull() ?: return null
        val title = dto.title.trim().takeIf { it.isNotBlank() } ?: return null

        if (dto.isAllDay) {
            return ParsedAppointmentDraft(
                title = title,
                date = date,
                isAllDay = true,
                startTime = null,
                endTime = null,
                parsedByGemini = true,
            )
        }

        val startTime = dto.startTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
            ?: return ParsedAppointmentDraft(
                title = title,
                date = date,
                isAllDay = true,
                startTime = null,
                endTime = null,
                parsedByGemini = true,
            )

        val parsedEnd = dto.endTime?.let { runCatching { LocalTime.parse(it) }.getOrNull() }
        val resolvedEnd = parsedEnd?.takeIf { it != startTime } ?: Appointment.defaultEndTime(startTime)

        return ParsedAppointmentDraft(
            title = title,
            date = date,
            isAllDay = false,
            startTime = startTime,
            endTime = resolvedEnd,
            parsedByGemini = true,
        )
    }

    @Serializable
    private data class GeminiGenerateRequest(
        val contents: List<GeminiContent>,
        val generationConfig: GeminiGenerationConfig,
    )

    @Serializable
    private data class GeminiContent(
        val parts: List<GeminiPart>,
    )

    @Serializable
    private data class GeminiPart(
        val text: String,
    )

    @Serializable
    private data class GeminiGenerationConfig(
        val responseMimeType: String,
        val temperature: Double,
    )

    @Serializable
    private data class GeminiGenerateResponse(
        val candidates: List<GeminiCandidate> = emptyList(),
    )

    @Serializable
    private data class GeminiCandidate(
        val content: GeminiContent? = null,
    )

    @Serializable
    internal data class GeminiAppointmentDto(
        val title: String,
        val date: String,
        val isAllDay: Boolean = false,
        val startTime: String? = null,
        val endTime: String? = null,
    )

    companion object {
        private const val GEMINI_ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
