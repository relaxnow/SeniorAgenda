package nl.senioragenda.app.speech

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
    val shouldLaunchSystemVoiceSheet: Boolean = false,
)

/**
 * Nederlandse spraakherkenning (`nl-NL`) voor het inspreken van nieuwe afspraken via
 * de knop "Afspraak toevoegen".
 *
 * Gebruikt primair Android [SpeechRecognizer] met automatische terugval op de ingebouwde
 * Google-spraakdialoog ([RecognizerIntent.ACTION_RECOGNIZE_SPEECH]) wanneer de inline-dienst
 * op een Samsung-tablet een taalpakket- of clientfout geeft.
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

    fun createSystemVoiceIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "nl-NL")
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "nl-NL")
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Spreek uw afspraak in het Nederlands in")
    }

    fun startListening(onFinalResult: (String) -> Unit) {
        if (!isAvailable) {
            _state.value = VoiceInputState(shouldLaunchSystemVoiceSheet = true)
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
                    val online = isNetworkAvailable()
                    when (error) {
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT,
                        -> {
                            _state.value = _state.value.copy(
                                isListening = false,
                                errorMessage = "Ik heb niets gehoord. Tik opnieuw op Spreek in en praat rustig richting de tablet.",
                            )
                        }

                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                            _state.value = _state.value.copy(
                                isListening = false,
                                errorMessage = "Microfoontoestemming is nodig om afspraken in te spreken.",
                            )
                        }

                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT,
                        -> {
                            _state.value = _state.value.copy(
                                isListening = false,
                                errorMessage = "Geen wifi-verbinding voor spraakherkenning. Verbind met wifi of typ de afspraak in.",
                            )
                        }

                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED,
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE,
                        -> {
                            if (!online) {
                                _state.value = _state.value.copy(
                                    isListening = false,
                                    errorMessage = "De tablet heeft geen wifi-verbinding en het offline Nederlandse spraakpakket ontbreekt. Verbind met wifi of typ de afspraak in.",
                                )
                            } else {
                                // Probeer automatisch de Google-spraakdialoog op deze tablet
                                _state.value = VoiceInputState(
                                    isListening = false,
                                    shouldLaunchSystemVoiceSheet = true,
                                )
                            }
                        }

                        else -> {
                            if (online) {
                                _state.value = VoiceInputState(
                                    isListening = false,
                                    shouldLaunchSystemVoiceSheet = true,
                                )
                            } else {
                                _state.value = _state.value.copy(
                                    isListening = false,
                                    errorMessage = "Spraakherkenning vereist wifi of een offline Nederlands spraakpakket. U kunt de afspraak wel intypen.",
                                )
                            }
                        }
                    }
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
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Spreek uw afspraak in")
        }

        _state.value = VoiceInputState(isListening = true)
        recognizer.startListening(intent)
    }

    fun onSystemVoiceSheetLaunched() {
        _state.value = _state.value.copy(shouldLaunchSystemVoiceSheet = false)
    }

    fun onSystemVoiceSheetResult(recognizedText: String?, wasCancelled: Boolean) {
        val text = recognizedText?.trim().orEmpty()
        when {
            text.isNotBlank() -> {
                _state.value = VoiceInputState(
                    isListening = false,
                    finalText = text,
                )
            }

            wasCancelled -> {
                _state.value = _state.value.copy(isListening = false)
            }

            else -> {
                _state.value = VoiceInputState(
                    isListening = false,
                    errorMessage = if (isNetworkAvailable()) {
                        "Ik heb niets verstaan. Probeer opnieuw of typ de afspraak in."
                    } else {
                        "Geen wifi-verbinding voor spraakherkenning. Verbind met wifi of typ de afspraak in."
                    },
                )
            }
        }
    }

    fun onSystemVoiceSheetUnavailable() {
        _state.value = VoiceInputState(
            isListening = false,
            errorMessage = "Spraakherkenning is niet beschikbaar op deze tablet. U kunt de afspraak wel intypen.",
        )
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

    private fun isNetworkAvailable(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
