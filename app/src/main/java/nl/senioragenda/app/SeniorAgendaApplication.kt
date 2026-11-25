package nl.senioragenda.app

import android.app.Application
import nl.senioragenda.app.data.AgendaRepository
import nl.senioragenda.app.data.local.AgendaPreferences
import nl.senioragenda.app.data.local.AndroidGoogleCalendarSyncClient
import nl.senioragenda.app.data.remote.GeminiAppointmentParser
import nl.senioragenda.app.data.remote.GoogleCalendarApiClient
import nl.senioragenda.app.data.remote.GoogleCalendarAuthManager
import nl.senioragenda.app.reminder.ReminderAlarmScheduler

class SeniorAgendaApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

class AppContainer(application: Application) {
    val preferences = AgendaPreferences(application)
    val androidGoogleCalendarSyncClient = AndroidGoogleCalendarSyncClient(application)
    val googleAuthManager = GoogleCalendarAuthManager(application)
    val googleCalendarApiClient = GoogleCalendarApiClient()
    val geminiAppointmentParser = GeminiAppointmentParser()
    val agendaRepository = AgendaRepository(
        preferences = preferences,
        androidGoogleCalendarSyncClient = androidGoogleCalendarSyncClient,
        googleCalendarApiClient = googleCalendarApiClient,
    )
    val reminderAlarmScheduler = ReminderAlarmScheduler(application)
}
