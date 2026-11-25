package nl.senioragenda.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import nl.senioragenda.app.reminder.ReminderAlarmScheduler
import nl.senioragenda.app.ui.AgendaDashboardScreen
import nl.senioragenda.app.ui.AgendaViewModel
import nl.senioragenda.app.ui.theme.SeniorAgendaTheme

class MainActivity : ComponentActivity() {

    private val viewModel: AgendaViewModel by viewModels {
        val app = application as SeniorAgendaApplication
        AgendaViewModel.factory(
            application = app,
            container = app.container,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        configureAlwaysOnTabletWindow()
        handleIncomingAlarmIntent(intent)

        setContent {
            SeniorAgendaTheme {
                val state by viewModel.uiState.collectAsStateWithLifecycle()

                // Pas schermhelderheid voor het TFT LCD-paneel dynamisch aan op de nachtstand
                LaunchedEffect(state.nightModeState.targetScreenBrightness) {
                    applyWindowBrightness(state.nightModeState.targetScreenBrightness)
                }

                // Microfoontoestemming aanvragen voor Nederlandse spraakinvoer
                val micPermissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission(),
                ) { granted ->
                    if (granted) {
                        viewModel.startDutchVoiceCapture()
                    } else {
                        viewModel.onMicPermissionDenied()
                    }
                }

                // OAuth-toestemmingsscherm van Google Play Services starten wanneer nodig
                val googleConsentLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartIntentSenderForResult(),
                ) { activityResult ->
                    viewModel.onGoogleConsentResult(activityResult.data)
                }

                LaunchedEffect(state.pendingConsentIntentSender) {
                    val intentSender = state.pendingConsentIntentSender ?: return@LaunchedEffect
                    viewModel.onConsentIntentLaunched()
                    googleConsentLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build(),
                    )
                }

                AgendaDashboardScreen(
                    state = state,
                    onSpeakClick = viewModel::onSpeakAgendaClicked,
                    onAddAppointmentClick = viewModel::openAddAppointmentSheet,
                    onSettingsClick = viewModel::openFamilySettings,
                    onReminderReadAgainClick = viewModel::onReminderReadAgain,
                    onReminderUnderstoodClick = viewModel::onReminderUnderstood,
                    onNightModeScreenTapped = viewModel::onNightModeScreenTapped,
                    onAddInputChanged = viewModel::onAddAppointmentTextChanged,
                    onStartVoiceClick = {
                        val hasMicPermission = ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.RECORD_AUDIO,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasMicPermission) {
                            viewModel.startDutchVoiceCapture()
                        } else {
                            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    onStopVoiceClick = viewModel::stopDutchVoiceCapture,
                    onParseAppointmentClick = viewModel::parseCurrentAppointmentInput,
                    onConfirmSaveAppointmentClick = viewModel::confirmSaveParsedAppointment,
                    onDismissAddSheet = viewModel::closeAddAppointmentSheet,
                    onCalendarIdChanged = viewModel::onCalendarIdChanged,
                    onSaveCalendarId = viewModel::saveCalendarIdAndRefresh,
                    onConnectGoogleClick = {
                        viewModel.connectOrRefreshGoogleCalendar(silentIfConsentNeeded = false)
                    },
                    onTestReminderClick = viewModel::triggerTestReminder,
                    onToggleNightPreviewClick = viewModel::toggleManualNightPreview,
                    onDismissSettingsSheet = viewModel::closeFamilySettings,
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingAlarmIntent(intent)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemBars()
        }
    }

    private fun handleIncomingAlarmIntent(intent: Intent?) {
        if (intent == null) return
        if (intent.hasExtra(ReminderAlarmScheduler.EXTRA_APPOINTMENT_ID)) {
            val appointmentId = intent.getStringExtra(ReminderAlarmScheduler.EXTRA_APPOINTMENT_ID)
            viewModel.handleAlarmWakeIntent(appointmentId)
        }
    }

    private fun configureAlwaysOnTabletWindow() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setShowWhenLocked(true)
        setTurnScreenOn(true)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun applyWindowBrightness(brightness: Float) {
        val params = window.attributes
        if (params.screenBrightness != brightness) {
            params.screenBrightness = brightness
            window.attributes = params
        }
    }
}
