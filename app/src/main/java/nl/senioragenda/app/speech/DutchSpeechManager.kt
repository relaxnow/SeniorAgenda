package nl.senioragenda.app.speech

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import nl.senioragenda.app.domain.formatting.DutchDateTimeFormatter
import java.util.UUID

enum class SpeechStatus {
    INITIALIZING,
    READY,
    SPEAKING,
    DUTCH_VOICE_MISSING,
    ERROR,
}

data class SpeechUiState(
    val status: SpeechStatus = SpeechStatus.INITIALIZING,
    val statusMessage: String? = null,
) {
    val isSpeaking: Boolean get() = status == SpeechStatus.SPEAKING
    val isDutchVoiceAvailable: Boolean
        get() = status == SpeechStatus.READY || status == SpeechStatus.SPEAKING
}

/**
 * Beheert Android [TextToSpeech] strikt in het Nederlands (`nl-NL`) met een rustig spreektempo.
 * Zorgt voor nette levenscyclus, stemcontrole en wachtrij voor vroege herinneringen.
 */
class DutchSpeechManager(
    context: Context,
) : TextToSpeech.OnInitListener {

    private val appContext = context.applicationContext
    private var tts: TextToSpeech? = null
    private var pendingUtterance: PendingUtterance? = null
    private var isShutdown = false

    private val _state = MutableStateFlow(SpeechUiState())
    val state: StateFlow<SpeechUiState> = _state.asStateFlow()

    init {
        tts = TextToSpeech(appContext, this)
    }

    override fun onInit(status: Int) {
        if (isShutdown) {
            tts?.shutdown()
            return
        }

        val engine = tts
        if (status != TextToSpeech.SUCCESS || engine == null) {
            _state.value = SpeechUiState(
                status = SpeechStatus.ERROR,
                statusMessage = "Spraakfunctie kon niet worden gestart.",
            )
            return
        }

        val availability = engine.isLanguageAvailable(DutchDateTimeFormatter.DUTCH_LOCALE)
        if (availability == TextToSpeech.LANG_MISSING_DATA ||
            availability == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            _state.value = SpeechUiState(
                status = SpeechStatus.DUTCH_VOICE_MISSING,
                statusMessage = "Nederlandse spraakgegevens ontbreken op deze tablet.",
            )
            return
        }

        engine.language = DutchDateTimeFormatter.DUTCH_LOCALE
        selectBestDutchVoice(engine)
        engine.setSpeechRate(CALM_SPEECH_RATE)
        engine.setPitch(NORMAL_PITCH)

        engine.setOnUtteranceProgressListener(
            object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _state.value = SpeechUiState(status = SpeechStatus.SPEAKING)
                }

                override fun onDone(utteranceId: String?) {
                    _state.value = SpeechUiState(status = SpeechStatus.READY)
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _state.value = SpeechUiState(
                        status = SpeechStatus.READY,
                        statusMessage = "Voorlezen is onderbroken.",
                    )
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _state.value = SpeechUiState(
                        status = SpeechStatus.READY,
                        statusMessage = "Voorlezen is onderbroken.",
                    )
                }

                override fun onStop(utteranceId: String?, interrupted: Boolean) {
                    _state.value = SpeechUiState(status = SpeechStatus.READY)
                }
            },
        )

        _state.value = SpeechUiState(status = SpeechStatus.READY)

        pendingUtterance?.let { queued ->
            pendingUtterance = null
            speak(queued.text, queued.isReminder)
        }
    }

    private fun selectBestDutchVoice(engine: TextToSpeech) {
        val voices = runCatching { engine.voices }.getOrNull().orEmpty()
        val dutchVoices = voices.filter { voice ->
            voice.locale.language.equals("nl", ignoreCase = true)
        }
        val preferred = dutchVoices.minWithOrNull(
            compareBy<android.speech.tts.Voice>(
                { !it.locale.country.equals("NL", ignoreCase = true) },
                { it.isNetworkConnectionRequired },
                { it.quality < android.speech.tts.Voice.QUALITY_HIGH },
            ),
        )
        if (preferred != null) {
            engine.voice = preferred
        }
    }

    fun speak(text: String, isReminder: Boolean = false) {
        if (isShutdown || text.isBlank()) return

        val engine = tts ?: return
        when (_state.value.status) {
            SpeechStatus.INITIALIZING -> {
                pendingUtterance = PendingUtterance(text = text, isReminder = isReminder)
                return
            }

            SpeechStatus.DUTCH_VOICE_MISSING,
            SpeechStatus.ERROR,
            -> return

            SpeechStatus.READY,
            SpeechStatus.SPEAKING,
            -> Unit
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .setUsage(
                if (isReminder) {
                    AudioAttributes.USAGE_ALARM
                } else {
                    AudioAttributes.USAGE_ASSISTANT
                },
            )
            .build()
        engine.setAudioAttributes(audioAttributes)

        val params = Bundle().apply {
            putInt(
                TextToSpeech.Engine.KEY_PARAM_STREAM,
                if (isReminder) AudioManager.STREAM_ALARM else AudioManager.STREAM_MUSIC,
            )
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        val utteranceId = "nl-agenda-${UUID.randomUUID()}"
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun stop() {
        tts?.stop()
        if (_state.value.status == SpeechStatus.SPEAKING) {
            _state.value = SpeechUiState(status = SpeechStatus.READY)
        }
    }

    fun shutdown() {
        isShutdown = true
        pendingUtterance = null
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    private data class PendingUtterance(
        val text: String,
        val isReminder: Boolean,
    )

    companion object {
        private const val CALM_SPEECH_RATE = 0.88f
        private const val NORMAL_PITCH = 1.0f
    }
}
