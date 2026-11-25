package nl.senioragenda.app.data.remote

import android.content.Context
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await

sealed interface GoogleAuthOutcome {
    data class Authorized(val accessToken: String) : GoogleAuthOutcome
    data class NeedsUserConsent(val intentSender: IntentSender) : GoogleAuthOutcome
    data class Failed(val dutchMessage: String) : GoogleAuthOutcome
}

/**
 * Beheert OAuth 2.0-autorisatie voor de Google Calendar API via Google Play Services
 * [Identity.getAuthorizationClient].
 */
class GoogleCalendarAuthManager(
    context: Context,
) {
    private val authorizationClient = Identity.getAuthorizationClient(context.applicationContext)

    private val calendarScopes = listOf(
        Scope(SCOPE_CALENDAR_EVENTS),
        Scope(SCOPE_CALENDAR_READONLY),
    )

    suspend fun authorize(): GoogleAuthOutcome {
        return try {
            val request = AuthorizationRequest.builder()
                .setRequestedScopes(calendarScopes)
                .build()

            val result = authorizationClient.authorize(request).await()
            val token = result.accessToken
            val pendingIntent = result.pendingIntent

            when {
                !token.isNullOrBlank() -> GoogleAuthOutcome.Authorized(token)
                result.hasResolution() && pendingIntent != null -> {
                    GoogleAuthOutcome.NeedsUserConsent(pendingIntent.intentSender)
                }
                else -> GoogleAuthOutcome.Failed(
                    "Geen toegangstoken ontvangen van Google. Controleer de OAuth-instellingen.",
                )
            }
        } catch (_: Exception) {
            GoogleAuthOutcome.Failed(
                "Koppelen met Google Agenda is niet gelukt. Controleer internet en Google-account op deze tablet.",
            )
        }
    }

    fun handleConsentResult(data: Intent?): GoogleAuthOutcome {
        if (data == null) {
            return GoogleAuthOutcome.Failed("Koppelen met Google Agenda is geannuleerd.")
        }
        return try {
            val result = authorizationClient.getAuthorizationResultFromIntent(data)
            val token = result.accessToken
            if (!token.isNullOrBlank()) {
                GoogleAuthOutcome.Authorized(token)
            } else {
                GoogleAuthOutcome.Failed("Toegang tot Google Agenda is niet verleend.")
            }
        } catch (_: Exception) {
            GoogleAuthOutcome.Failed("Kon de goedkeuring van Google Agenda niet verwerken.")
        }
    }

    companion object {
        private const val SCOPE_CALENDAR_EVENTS = "https://www.googleapis.com/auth/calendar.events"
        private const val SCOPE_CALENDAR_READONLY = "https://www.googleapis.com/auth/calendar.readonly"
    }
}
