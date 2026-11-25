package nl.senioragenda.app.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceInputState(
    val isListening: Boolean = false,
    val partialText: String = "",
    val finalText: String = "",
    val errorMessage: String? = null,
)

/**
 * Nederlandse spraakherkenning (`nl-NL`) voor het inspreken van nieuwe afspraken via
 * de knop "Afspraak toevoegen".
 */
class DutchVoiceInputManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private var speechRecognizer: SpeechRecognizer? = null

    private val _state = MutableStateFlow(VoiceInputState())
    val state: StateFlow<VoiceInputState> = _state.asStateFlow()

    val isAvailable: Boolean
        get() = SpeechRecognizer.isRecognitionAvailable(appContext)

    fun startListening(onFinalResult: (String) -> Unit) {
        if (!isAvailable) {
            _state.value = VoiceInputState(
                errorMessage = "Spraakherkenning is niet beschikbaar op deze tablet. U kunt de afspraak wel intypen.",
            )
            return
        }

        stopListening()

        val recognizer = SpeechRecognizer.createSpeechRecognizer(appContext)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(
            object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    _state.value = VoiceInputState(isListening = true)
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    _state.value = _state.value.copy(isListening = false)
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        -> "Ik heb niets gehoord. Probeer het nog een keer rustig in te spreken."

                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                            "Microfoontoestemming is nodig om afspraken in te spreken."

                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                        -> "Geen internetverbinding voor spraakherkenning. U kunt de afspraak wel intypen."

                        else -> "Spraakherkenning kon niet worden voltooid. Probeer opnieuw of typ de afspraak in."
                    }
                    _state.value = _state.value.copy(
                        isListening = false,
                        errorMessage = message,
                    )
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val best = matches?.firstOrNull()?.trim().orEmpty()
                    _state.value = VoiceInputState(
                        isListening = false,
                        finalText = best,
                    )
                    if (best.isNotBlank()) {
                        onFinalResult(best)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()?.trim().orEmpty()
                    if (partial.isNotBlank()) {
                        _state.value = _state.value.copy(partialText = partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            },
        )

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "nl-NL")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "nl-NL")
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Spreek uw afspraak in")
        }

        _state.value = VoiceInputState(isListening = true)
        recognizer.startListening(intent)
    }

    fun stopListening() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
        _state.value = _state.value.copy(isListening = false)
    }

    fun reset() {
        stopListening()
        _state.value = VoiceInputState()
    }
}
