package nl.senioragenda.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import nl.senioragenda.app.nightmode.NightModeState
import nl.senioragenda.app.ui.components.AddAppointmentModal
import nl.senioragenda.app.ui.components.FamilySettingsModal
import nl.senioragenda.app.ui.components.NightModeRestOverlay
import nl.senioragenda.app.ui.components.ReminderOverlay
import nl.senioragenda.app.ui.components.TemporaryWakeBanner
import nl.senioragenda.app.ui.components.TodayPanel
import nl.senioragenda.app.ui.components.TomorrowPanel
import nl.senioragenda.app.ui.components.TopDashboardBar
import nl.senioragenda.app.ui.components.UpcomingDaysPanel
import nl.senioragenda.app.ui.theme.AgendaColors

@Composable
fun AgendaDashboardScreen(
    state: AgendaUiState,
    onSpeakClick: () -> Unit,
    onAddAppointmentClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onReminderReadAgainClick: () -> Unit,
    onReminderUnderstoodClick: () -> Unit,
    onNightModeScreenTapped: () -> Unit,
    onAddInputChanged: (String) -> Unit,
    onStartVoiceClick: () -> Unit,
    onStopVoiceClick: () -> Unit,
    onParseAppointmentClick: () -> Unit,
    onConfirmSaveAppointmentClick: () -> Unit,
    onDismissAddSheet: () -> Unit,
    onCalendarIdChanged: (String) -> Unit,
    onSaveCalendarId: () -> Unit,
    onSyncDeviceCalendarClick: () -> Unit,
    onConnectGoogleClick: () -> Unit,
    onTestReminderClick: () -> Unit,
    onToggleNightPreviewClick: () -> Unit,
    onDismissSettingsSheet: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgendaColors.Background),
    ) {
        // Hoofddashboard in vaste liggende drie-sectie-indeling
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            TopDashboardBar(
                now = state.now,
                syncStatus = state.syncStatus,
                syncStatusLabel = state.syncStatusLabel,
                speechState = state.speechState,
                onSpeakClick = onSpeakClick,
                onAddAppointmentClick = onAddAppointmentClick,
                onSettingsClick = onSettingsClick,
            )

            val tempWake = state.nightModeState as? NightModeState.TemporaryWake
            if (tempWake != null) {
                Spacer(modifier = Modifier.height(10.dp))
                TemporaryWakeBanner(secondsRemaining = tempWake.secondsRemaining)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // Links: VANDAAG (hoofdpaneel)
                TodayPanel(
                    todayAgenda = state.snapshot.today,
                    currentTime = state.now.toLocalTime(),
                    modifier = Modifier.weight(0.56f),
                )

                // Rechts: MORGEN (boven) en DAARNA (onder)
                Column(
                    modifier = Modifier
                        .weight(0.44f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(18.dp),
                ) {
                    TomorrowPanel(
                        tomorrowAgenda = state.snapshot.tomorrow,
                        modifier = Modifier.weight(0.42f),
                    )
                    UpcomingDaysPanel(
                        upcomingDays = state.snapshot.upcomingDays,
                        modifier = Modifier.weight(0.58f),
                    )
                }
            }
        }

        // Nachtstand-rustscherm (23:30 - 05:30), behalve wanneer een herinnering of tijdelijke wake actief is
        if (state.nightModeState is NightModeState.Sleeping) {
            NightModeRestOverlay(
                now = state.now,
                isManualPreview = state.manualNightPreview,
                onTapToWake = onNightModeScreenTapped,
            )
        }

        // Venster voor familiebeheer & Google Agenda koppeling
        if (state.settingsSheet.isOpen && state.activeReminder == null) {
            FamilySettingsModal(
                state = state.settingsSheet,
                manualNightPreview = state.manualNightPreview,
                onCalendarIdChanged = onCalendarIdChanged,
                onSaveCalendarId = onSaveCalendarId,
                onSyncDeviceCalendarClick = onSyncDeviceCalendarClick,
                onConnectGoogleClick = onConnectGoogleClick,
                onTestReminderClick = onTestReminderClick,
                onToggleNightPreviewClick = onToggleNightPreviewClick,
                onDismissClick = onDismissSettingsSheet,
            )
        }

        // Venster voor "Afspraak toevoegen" (Nederlandse spraak + Gemini)
        if (state.addSheet.isOpen && state.activeReminder == null) {
            AddAppointmentModal(
                state = state.addSheet,
                voiceState = state.voiceInputState,
                isGeminiConfigured = state.isGeminiConfigured,
                onInputChanged = onAddInputChanged,
                onStartVoiceClick = onStartVoiceClick,
                onStopVoiceClick = onStopVoiceClick,
                onParseClick = onParseAppointmentClick,
                onConfirmSaveClick = onConfirmSaveAppointmentClick,
                onDismissClick = onDismissAddSheet,
            )
        }

        // Hoogste prioriteit: persistente herinneringsoverlay (doorbreekt altijd nachtstand)
        state.activeReminder?.let { activeReminder ->
            ReminderOverlay(
                reminder = activeReminder,
                now = state.now,
                onReadAgainClick = onReminderReadAgainClick,
                onUnderstoodClick = onReminderUnderstoodClick,
            )
        }
    }
}
