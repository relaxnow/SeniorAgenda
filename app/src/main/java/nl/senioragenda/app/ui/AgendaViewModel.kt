package nl.senioragenda.app.ui

import android.app.Application
import android.content.Intent
import android.content.IntentSender
import android.database.ContentObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import nl.senioragenda.app.AppContainer
import nl.senioragenda.app.data.AgendaRepository
import nl.senioragenda.app.data.remote.GoogleAuthOutcome
import nl.senioragenda.app.domain.formatting.DutchDateTimeFormatter
import nl.senioragenda.app.domain.formatting.DutchSpeechScriptBuilder
import nl.senioragenda.app.domain.model.AgendaSnapshot
import nl.senioragenda.app.domain.model.Appointment
import nl.senioragenda.app.domain.model.AppointmentSource
import nl.senioragenda.app.domain.model.ParsedAppointmentDraft
import nl.senioragenda.app.nightmode.NightModePolicy
import nl.senioragenda.app.nightmode.NightModeState
import nl.senioragenda.app.reminder.ActiveReminder
import nl.senioragenda.app.reminder.ReminderPolicy
import nl.senioragenda.app.speech.DutchSpeechManager
import nl.senioragenda.app.speech.DutchVoiceInputManager
import nl.senioragenda.app.speech.SpeechUiState
import nl.senioragenda.app.speech.VoiceInputState
import java.time.LocalDate
import java.time.LocalDateTime

enum class CalendarSyncStatus {
    DEMO_MODE,
    SYNCING,
    CONNECTED,
    ERROR,
}

data class AddAppointmentSheetState(
    val isOpen: Boolean = false,
    val inputText: String = "",
    val isParsing: Boolean = false,
    val isSaving: Boolean = false,
    val parsedDraft: ParsedAppointmentDraft? = null,
    val feedbackMessage: String? = null,
)

data class FamilySettingsState(
    val isOpen: Boolean = false,
    val calendarIdInput: String = "primary",
    val statusNotice: String? = null,
)

data class AgendaUiState(
    val now: LocalDateTime = LocalDateTime.now(),
    val snapshot: AgendaSnapshot = AgendaRepository.buildSnapshot(LocalDate.now(), emptyList()),
    val syncStatus: CalendarSyncStatus = CalendarSyncStatus.DEMO_MODE,
    val syncStatusLabel: String = "Voorbeeldweergave · Beheer",
    val lastSyncTimeLabel: String? = null,
    val speechState: SpeechUiState = SpeechUiState(),
    val voiceInputState: VoiceInputState = VoiceInputState(),
    val activeReminder: ActiveReminder? = null,
    val nightModeState: NightModeState = NightModeState.Daytime,
    val manualNightPreview: Boolean = false,
    val addSheet: AddAppointmentSheetState = AddAppointmentSheetState(),
    val settingsSheet: FamilySettingsState = FamilySettingsState(),
    val pendingConsentIntentSender: IntentSender? = null,
    val isGeminiConfigured: Boolean = false,
    val usingDeviceGoogleCalendar: Boolean = false,
)

class AgendaViewModel(
    private val container: AppContainer,
    private val speechManager: DutchSpeechManager,
    private val voiceInputManager: DutchVoiceInputManager,
) : ViewModel() {

    private var allAppointments: List<Appointment> = emptyList()
    private var googleAccessToken: String? = null
    private var temporaryWakeUntil: LocalDateTime? = null
    private val acknowledgedIds: MutableSet<String> =
        container.preferences.getAcknowledgedReminderIds().toMutableSet()
    private val lastSpokenSlotByAppointmentId: MutableMap<String, Int> = mutableMapOf()
    private var manualTestReminder: ActiveReminder? = null
    private var lastManualTestSpokenAt: LocalDateTime? = null
    private var lastDisplayedDate: LocalDate = LocalDate.now()
    private var calendarContentObserver: ContentObserver? = null

    private val _uiState = MutableStateFlow(
        AgendaUiState(
            isGeminiConfigured = container.geminiAppointmentParser.isGeminiConfigured,
            settingsSheet = FamilySettingsState(calendarIdInput = container.preferences.calendarId),
        ),
    )
    val uiState: StateFlow<AgendaUiState> = _uiState.asStateFlow()

    init {
        val now = LocalDateTime.now()
        lastDisplayedDate = now.toLocalDate()

        observeSpeech()
        observeVoiceInput()
        startClockAndReminderLoop()
        startPeriodicCalendarSync()

        if (container.agendaRepository.hasDeviceCalendarPermission()) {
            ensureCalendarObserverRegistered()
            refreshDeviceSyncedGoogleCalendar()
        } else {
            applyAppointments(
                appointments = container.agendaRepository.loadLocalFallbackAppointments(now.toLocalDate()),
                now = now,
            )
            if (container.preferences.isGoogleCalendarConnected) {
                connectOrRefreshGoogleCalendar(silentIfConsentNeeded = true)
            }
        }
    }

    private fun observeSpeech() {
        viewModelScope.launch {
            speechManager.state.collect { speechUiState ->
                _uiState.update { it.copy(speechState = speechUiState) }
            }
        }
    }

    private fun observeVoiceInput() {
        viewModelScope.launch {
            voiceInputManager.state.collect { voiceState ->
                _uiState.update { current ->
                    val updatedInput = when {
                        voiceState.finalText.isNotBlank() -> voiceState.finalText
                        voiceState.partialText.isNotBlank() -> voiceState.partialText
                        else -> current.addSheet.inputText
                    }
                    current.copy(
                        voiceInputState = voiceState,
                        addSheet = current.addSheet.copy(
                            inputText = updatedInput,
                            feedbackMessage = voiceState.errorMessage ?: current.addSheet.feedbackMessage,
                        ),
                    )
                }
            }
        }
    }

    private fun startClockAndReminderLoop() {
        viewModelScope.launch {
            while (isActive) {
                tick(LocalDateTime.now())
                delay(CLOCK_TICK_MS)
            }
        }
    }

    private fun startPeriodicCalendarSync() {
        viewModelScope.launch {
            while (isActive) {
                delay(CALENDAR_SYNC_INTERVAL_MS)
                when {
                    googleAccessToken != null -> refreshGoogleCalendarWithToken(googleAccessToken!!)
                    container.agendaRepository.hasDeviceCalendarPermission() -> {
                        container.androidGoogleCalendarSyncClient.requestImmediateGoogleSync()
                        refreshDeviceSyncedGoogleCalendar()
                    }
                    container.preferences.isGoogleCalendarConnected -> {
                        connectOrRefreshGoogleCalendar(silentIfConsentNeeded = true)
                    }
                }
            }
        }
    }

    private fun tick(now: LocalDateTime) {
        if (now.toLocalDate() != lastDisplayedDate) {
            lastDisplayedDate = now.toLocalDate()
            when {
                googleAccessToken != null -> refreshGoogleCalendarWithToken(googleAccessToken!!)
                container.agendaRepository.hasDeviceCalendarPermission() -> refreshDeviceSyncedGoogleCalendar()
                else -> applyAppointments(
                    appointments = container.agendaRepository.loadLocalFallbackAppointments(now.toLocalDate()),
                    now = now,
                )
            }
        }

        val currentReminder = resolveActiveReminder(now)
        val nightState = NightModePolicy.resolve(
            now = now,
            hasActiveReminder = currentReminder != null,
            temporaryWakeUntil = temporaryWakeUntil,
            manualNightPreview = _uiState.value.manualNightPreview,
        )

        if (nightState is NightModeState.Daytime && temporaryWakeUntil != null) {
            temporaryWakeUntil = null
        }

        _uiState.update { state ->
            state.copy(
                now = now,
                activeReminder = currentReminder,
                nightModeState = nightState,
            )
        }
    }

    private fun resolveActiveReminder(now: LocalDateTime): ActiveReminder? {
        val manual = manualTestReminder
        if (manual != null) {
            val lastSpoken = lastManualTestSpokenAt
            if (lastSpoken == null || !now.isBefore(lastSpoken.plusMinutes(ReminderPolicy.REPEAT_INTERVAL_MINUTES))) {
                lastManualTestSpokenAt = now
                speakReminder(manual.appointment, now)
            }
            return manual
        }

        val currentlyDisplayedId = _uiState.value.activeReminder?.appointment?.id
        val evaluation = ReminderPolicy.evaluate(
            appointments = allAppointments,
            now = now,
            acknowledgedIds = acknowledgedIds,
            lastSpokenSlotById = lastSpokenSlotByAppointmentId,
            currentlyDisplayedId = currentlyDisplayedId,
        )

        val appointment = evaluation.activeAppointment ?: return null
        val slot = evaluation.currentSlotIndex ?: 0

        if (evaluation.shouldSpeakNow) {
            lastSpokenSlotByAppointmentId[appointment.id] = slot
            speakReminder(appointment, now)
            scheduleNextSystemAlarm(now)
        }

        return ActiveReminder(
            appointment = appointment,
            triggeredSlotIndex = slot,
            isManualTest = false,
        )
    }

    private fun speakReminder(appointment: Appointment, now: LocalDateTime) {
        val text = DutchSpeechScriptBuilder.buildReminderAnnouncement(appointment, now)
        speechManager.speak(text = text, isReminder = true)
    }

    private fun applyAppointments(
        appointments: List<Appointment>,
        now: LocalDateTime,
    ) {
        allAppointments = appointments.sortedWith(Appointment.CHRONOLOGICAL_COMPARATOR)
        val snapshot = AgendaRepository.buildSnapshot(now.toLocalDate(), allAppointments)
        _uiState.update { it.copy(snapshot = snapshot, now = now) }
        scheduleNextSystemAlarm(now)
    }

    private fun scheduleNextSystemAlarm(now: LocalDateTime) {
        container.reminderAlarmScheduler.scheduleNextReminder(
            appointments = allAppointments,
            now = now,
            acknowledgedIds = acknowledgedIds,
        )
    }

    private fun ensureCalendarObserverRegistered() {
        if (calendarContentObserver == null) {
            calendarContentObserver = container.androidGoogleCalendarSyncClient.registerCalendarObserver {
                refreshDeviceSyncedGoogleCalendar()
            }
        }
    }

    // --- Live Google Agenda op deze Android-tablet (`CalendarContract`) ---

    fun onCalendarPermissionsResult(granted: Boolean) {
        if (granted) {
            ensureCalendarObserverRegistered()
            refreshDeviceSyncedGoogleCalendar()
        } else {
            _uiState.update {
                it.copy(
                    settingsSheet = it.settingsSheet.copy(
                        statusNotice = "Agendatoestemming is nodig om uw echte Google Agenda te tonen.",
                    ),
                )
            }
        }
    }

    fun refreshDeviceSyncedGoogleCalendar(triggerCloudSync: Boolean = false) {
        viewModelScope.launch {
            if (triggerCloudSync) {
                container.androidGoogleCalendarSyncClient.requestImmediateGoogleSync()
            }
            _uiState.update {
                it.copy(
                    syncStatus = CalendarSyncStatus.SYNCING,
                    syncStatusLabel = "Google Agenda bijwerken…",
                )
            }

            val now = LocalDateTime.now()
            val result = runCatching {
                container.agendaRepository.fetchSyncedDeviceGoogleCalendar(today = now.toLocalDate())
            }

            result.onSuccess { loadResult ->
                if (loadResult.hasSyncedGoogleCalendars) {
                    applyAppointments(loadResult.appointments, now)
                    val timeStr = DutchDateTimeFormatter.formatTime(now.toLocalTime())
                    val accountLabel = loadResult.accountLabel ?: "Google Agenda"
                    _uiState.update {
                        it.copy(
                            syncStatus = CalendarSyncStatus.CONNECTED,
                            syncStatusLabel = "Google Agenda ($accountLabel) · $timeStr",
                            lastSyncTimeLabel = timeStr,
                            usingDeviceGoogleCalendar = true,
                            settingsSheet = it.settingsSheet.copy(
                                statusNotice = "Gekoppeld met Google Agenda ($accountLabel). Bijgewerkt om $timeStr.",
                            ),
                        )
                    }
                } else {
                    applyAppointments(
                        appointments = container.agendaRepository.loadLocalFallbackAppointments(now.toLocalDate()),
                        now = now,
                    )
                    _uiState.update {
                        it.copy(
                            syncStatus = CalendarSyncStatus.DEMO_MODE,
                            syncStatusLabel = "Geen Google Agenda gevonden · Beheer",
                            usingDeviceGoogleCalendar = false,
                            settingsSheet = it.settingsSheet.copy(
                                statusNotice = "Geen gesynchroniseerde Google Agenda gevonden op deze tablet. Controleer de Google Agenda-app.",
                            ),
                        )
                    }
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        syncStatus = CalendarSyncStatus.ERROR,
                        syncStatusLabel = "Agenda lezen mislukt · Beheer",
                        settingsSheet = it.settingsSheet.copy(
                            statusNotice = "Kon de agenda op deze tablet niet uitlezen. Controleer agendatoestemming.",
                        ),
                    )
                }
            }
        }
    }

    // --- Gebruikersacties: Voorlezen ("Spreek voor") ---

    fun onSpeakAgendaClicked() {
        registerUserActivityIfTemporaryWake()
        if (_uiState.value.speechState.isSpeaking) {
            speechManager.stop()
            return
        }
        val state = _uiState.value
        val script = DutchSpeechScriptBuilder.buildDailyBriefing(
            snapshot = state.snapshot,
            now = state.now,
        )
        speechManager.speak(text = script, isReminder = false)
    }

    // --- Gebruikersacties: Herinneringsoverlay ---

    fun onReminderUnderstood() {
        val active = _uiState.value.activeReminder ?: return
        speechManager.stop()

        if (active.isManualTest) {
            manualTestReminder = null
            lastManualTestSpokenAt = null
        } else {
            acknowledgedIds.add(active.appointment.id)
            container.preferences.markReminderAcknowledged(active.appointment.id)
        }

        val now = LocalDateTime.now()
        scheduleNextSystemAlarm(now)
        tick(now)
    }

    fun onReminderReadAgain() {
        val active = _uiState.value.activeReminder ?: return
        speakReminder(active.appointment, LocalDateTime.now())
    }

    fun handleAlarmWakeIntent(appointmentId: String?) {
        val now = LocalDateTime.now()
        if (appointmentId != null) {
            lastSpokenSlotByAppointmentId.remove(appointmentId)
        }
        tick(now)
    }

    // --- Gebruikersacties: Nachtstand (TFT LCD) ---

    fun onNightModeScreenTapped() {
        val now = LocalDateTime.now()
        temporaryWakeUntil = now.plusSeconds(NightModePolicy.TEMPORARY_WAKE_SECONDS)
        tick(now)
    }

    private fun registerUserActivityIfTemporaryWake() {
        if (_uiState.value.nightModeState is NightModeState.TemporaryWake) {
            temporaryWakeUntil = LocalDateTime.now().plusSeconds(NightModePolicy.TEMPORARY_WAKE_SECONDS)
        }
    }

    fun toggleManualNightPreview() {
        val enabling = !_uiState.value.manualNightPreview
        temporaryWakeUntil = null
        _uiState.update {
            it.copy(
                manualNightPreview = enabling,
                settingsSheet = it.settingsSheet.copy(isOpen = false),
            )
        }
        tick(LocalDateTime.now())
    }

    // --- Gebruikersacties: Afspraak toevoegen (Nederlands + Gemini) ---

    fun openAddAppointmentSheet() {
        registerUserActivityIfTemporaryWake()
        voiceInputManager.reset()
        _uiState.update {
            it.copy(
                addSheet = AddAppointmentSheetState(isOpen = true),
            )
        }
    }

    fun closeAddAppointmentSheet() {
        voiceInputManager.reset()
        _uiState.update {
            it.copy(addSheet = AddAppointmentSheetState(isOpen = false))
        }
    }

    fun onAddAppointmentTextChanged(newText: String) {
        _uiState.update {
            it.copy(
                addSheet = it.addSheet.copy(
                    inputText = newText,
                    feedbackMessage = null,
                ),
            )
        }
    }

    fun startDutchVoiceCapture() {
        speechManager.stop()
        _uiState.update {
            it.copy(
                addSheet = it.addSheet.copy(
                    feedbackMessage = null,
                    parsedDraft = null,
                ),
            )
        }
        voiceInputManager.startListening { finalSpokenText ->
            _uiState.update {
                it.copy(addSheet = it.addSheet.copy(inputText = finalSpokenText))
            }
            parseCurrentAppointmentInput()
        }
    }

    fun stopDutchVoiceCapture() {
        voiceInputManager.stopListening()
    }

    fun createSystemVoiceIntent(): Intent = voiceInputManager.createSystemVoiceIntent()

    fun onSystemVoiceSheetLaunched() {
        voiceInputManager.onSystemVoiceSheetLaunched()
    }

    fun onSystemVoiceSheetResult(recognizedText: String?, wasCancelled: Boolean) {
        voiceInputManager.onSystemVoiceSheetResult(recognizedText, wasCancelled)
        val trimmed = recognizedText?.trim().orEmpty()
        if (trimmed.isNotBlank()) {
            _uiState.update {
                it.copy(
                    addSheet = it.addSheet.copy(
                        inputText = trimmed,
                        feedbackMessage = null,
                    ),
                )
            }
            parseCurrentAppointmentInput()
        }
    }

    fun onSystemVoiceSheetUnavailable() {
        voiceInputManager.onSystemVoiceSheetUnavailable()
    }

    fun parseCurrentAppointmentInput() {
        val rawText = _uiState.value.addSheet.inputText.trim()
        if (rawText.isBlank()) {
            _uiState.update {
                it.copy(
                    addSheet = it.addSheet.copy(
                        feedbackMessage = "Spreek of typ eerst een afspraak in, bijvoorbeeld: Morgen om 14:00 huisarts.",
                    ),
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    addSheet = it.addSheet.copy(
                        isParsing = true,
                        feedbackMessage = null,
                        parsedDraft = null,
                    ),
                )
            }

            val draft = container.geminiAppointmentParser.parseDutchInput(
                dutchText = rawText,
                referenceNow = LocalDateTime.now(),
            )

            if (draft == null) {
                _uiState.update {
                    it.copy(
                        addSheet = it.addSheet.copy(
                            isParsing = false,
                            feedbackMessage = "Ik kon de afspraak niet goed herkennen. Noem een dag, tijd en onderwerp.",
                        ),
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        addSheet = it.addSheet.copy(
                            isParsing = false,
                            parsedDraft = draft,
                            feedbackMessage = null,
                        ),
                    )
                }
            }
        }
    }

    fun confirmSaveParsedAppointment() {
        val draft = _uiState.value.addSheet.parsedDraft ?: return
        viewModelScope.launch {
            _uiState.update {
                it.copy(addSheet = it.addSheet.copy(isSaving = true, feedbackMessage = null))
            }

            val result = runCatching {
                container.agendaRepository.addAppointment(
                    draft = draft,
                    accessToken = googleAccessToken,
                )
            }

            result.onSuccess { created ->
                val now = LocalDateTime.now()
                when {
                    googleAccessToken != null -> refreshGoogleCalendarWithToken(googleAccessToken!!)
                    container.agendaRepository.hasDeviceCalendarPermission() -> refreshDeviceSyncedGoogleCalendar()
                    else -> {
                        val merged = (allAppointments + created).distinctBy { it.id }
                        applyAppointments(merged, now)
                    }
                }
                closeAddAppointmentSheet()
                val dateLabel = DutchDateTimeFormatter.formatSpokenDate(created.date)
                val timeAnnouncement = if (created.isAllDay) {
                    "voor de hele dag"
                } else {
                    val start = DutchDateTimeFormatter.formatSpokenTime(requireNotNull(created.startTime))
                    val end = DutchDateTimeFormatter.formatSpokenTime(requireNotNull(created.endTime))
                    "van $start tot $end"
                }
                speechManager.speak("De afspraak ${created.title} op $dateLabel $timeAnnouncement is opgeslagen.")
            }.onFailure {
                _uiState.update {
                    it.copy(
                        addSheet = it.addSheet.copy(
                            isSaving = false,
                            feedbackMessage = "Opslaan in de agenda is niet gelukt. Controleer de agendatoestemming en probeer opnieuw.",
                        ),
                    )
                }
            }
        }
    }

    // --- Beheer & Google Agenda koppeling (voor familieleden) ---

    fun openFamilySettings() {
        registerUserActivityIfTemporaryWake()
        _uiState.update {
            it.copy(
                settingsSheet = FamilySettingsState(
                    isOpen = true,
                    calendarIdInput = container.preferences.calendarId,
                ),
            )
        }
    }

    fun closeFamilySettings() {
        _uiState.update {
            it.copy(settingsSheet = it.settingsSheet.copy(isOpen = false, statusNotice = null))
        }
    }

    fun onCalendarIdChanged(newId: String) {
        _uiState.update {
            it.copy(settingsSheet = it.settingsSheet.copy(calendarIdInput = newId))
        }
    }

    fun saveCalendarIdAndRefresh() {
        val newId = _uiState.value.settingsSheet.calendarIdInput.trim().ifBlank { "primary" }
        container.preferences.calendarId = newId
        _uiState.update {
            it.copy(
                settingsSheet = it.settingsSheet.copy(
                    calendarIdInput = newId,
                    statusNotice = "Agenda-ID opgeslagen.",
                ),
            )
        }
        if (googleAccessToken != null) {
            refreshGoogleCalendarWithToken(googleAccessToken!!)
        }
    }

    fun connectOrRefreshGoogleCalendar(silentIfConsentNeeded: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    syncStatus = CalendarSyncStatus.SYNCING,
                    syncStatusLabel = "Agenda bijwerken…",
                )
            }

            when (val outcome = container.googleAuthManager.authorize()) {
                is GoogleAuthOutcome.Authorized -> {
                    googleAccessToken = outcome.accessToken
                    container.preferences.isGoogleCalendarConnected = true
                    refreshGoogleCalendarWithToken(outcome.accessToken)
                }

                is GoogleAuthOutcome.NeedsUserConsent -> {
                    if (silentIfConsentNeeded) {
                        _uiState.update {
                            it.copy(
                                syncStatus = CalendarSyncStatus.DEMO_MODE,
                                syncStatusLabel = "Voorbeeldweergave · Beheer",
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(pendingConsentIntentSender = outcome.intentSender)
                        }
                    }
                }

                is GoogleAuthOutcome.Failed -> {
                    _uiState.update {
                        it.copy(
                            syncStatus = CalendarSyncStatus.ERROR,
                            syncStatusLabel = if (it.usingDeviceGoogleCalendar) it.syncStatusLabel else "Voorbeeldweergave · Beheer",
                            settingsSheet = it.settingsSheet.copy(statusNotice = outcome.dutchMessage),
                        )
                    }
                }
            }
        }
    }

    fun onConsentIntentLaunched() {
        _uiState.update { it.copy(pendingConsentIntentSender = null) }
    }

    fun onGoogleConsentResult(data: Intent?) {
        when (val outcome = container.googleAuthManager.handleConsentResult(data)) {
            is GoogleAuthOutcome.Authorized -> {
                googleAccessToken = outcome.accessToken
                container.preferences.isGoogleCalendarConnected = true
                refreshGoogleCalendarWithToken(outcome.accessToken)
            }

            is GoogleAuthOutcome.Failed -> {
                _uiState.update {
                    it.copy(
                        syncStatus = CalendarSyncStatus.ERROR,
                        syncStatusLabel = if (it.usingDeviceGoogleCalendar) it.syncStatusLabel else "Voorbeeldweergave · Beheer",
                        settingsSheet = it.settingsSheet.copy(statusNotice = outcome.dutchMessage),
                    )
                }
            }

            is GoogleAuthOutcome.NeedsUserConsent -> Unit
        }
    }

    private fun refreshGoogleCalendarWithToken(accessToken: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    syncStatus = CalendarSyncStatus.SYNCING,
                    syncStatusLabel = "Agenda bijwerken…",
                )
            }

            val now = LocalDateTime.now()
            val result = runCatching {
                container.agendaRepository.fetchGoogleCalendarAppointments(
                    accessToken = accessToken,
                    today = now.toLocalDate(),
                )
            }

            result.onSuccess { fetched ->
                applyAppointments(fetched, now)
                val timeStr = DutchDateTimeFormatter.formatTime(now.toLocalTime())
                _uiState.update {
                    it.copy(
                        syncStatus = CalendarSyncStatus.CONNECTED,
                        syncStatusLabel = "Google Agenda gekoppeld · $timeStr",
                        lastSyncTimeLabel = timeStr,
                        settingsSheet = it.settingsSheet.copy(
                            statusNotice = "Google Agenda is bijgewerkt om $timeStr.",
                        ),
                    )
                }
            }.onFailure {
                _uiState.update {
                    it.copy(
                        syncStatus = CalendarSyncStatus.ERROR,
                        syncStatusLabel = "Geen verbinding · Laatste overzicht actief",
                        settingsSheet = it.settingsSheet.copy(
                            statusNotice = "Kon Google Agenda niet ophalen. Controleer wifi en Agenda-ID.",
                        ),
                    )
                }
            }
        }
    }

    fun triggerTestReminder() {
        val now = LocalDateTime.now()
        val testStart = now.plusMinutes(30).withSecond(0).withNano(0)
        val startTime = testStart.toLocalTime()
        val endTime = Appointment.defaultEndTime(startTime)
        val demoAppointment = Appointment.timed(
            id = "test-reminder-${System.currentTimeMillis()}",
            title = "Huisarts",
            date = testStart.toLocalDate(),
            startTime = startTime,
            endTime = endTime,
            source = AppointmentSource.LOCAL,
        )
        manualTestReminder = ActiveReminder(
            appointment = demoAppointment,
            triggeredSlotIndex = 0,
            isManualTest = true,
        )
        lastManualTestSpokenAt = null
        _uiState.update {
            it.copy(settingsSheet = it.settingsSheet.copy(isOpen = false))
        }
        tick(now)
    }

    fun onMicPermissionDenied() {
        _uiState.update {
            it.copy(
                addSheet = it.addSheet.copy(
                    feedbackMessage = "Microfoontoestemming is geweigerd. U kunt de afspraak wel intypen.",
                ),
            )
        }
    }

    override fun onCleared() {
        container.androidGoogleCalendarSyncClient.unregisterCalendarObserver(calendarContentObserver)
        calendarContentObserver = null
        voiceInputManager.stopListening()
        speechManager.shutdown()
        super.onCleared()
    }

    companion object {
        private const val CLOCK_TICK_MS = 1_000L
        private const val CALENDAR_SYNC_INTERVAL_MS = 2 * 60 * 1_000L

        fun factory(
            application: Application,
            container: AppContainer,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AgendaViewModel(
                    container = container,
                    speechManager = DutchSpeechManager(application),
                    voiceInputManager = DutchVoiceInputManager(application),
                ) as T
            }
        }
    }
}
