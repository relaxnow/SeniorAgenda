package nl.senioragenda.app.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.senioragenda.app.domain.formatting.DutchDateTimeFormatter
import nl.senioragenda.app.domain.formatting.DutchSpeechScriptBuilder
import nl.senioragenda.app.reminder.ActiveReminder
import nl.senioragenda.app.ui.theme.AgendaColors
import java.time.LocalDateTime

/**
 * Prominente, hoog-contrast herinneringsoverlay over een gedimd dashboard.
 *
 * Gedragsregels:
 * - Tikken buiten de kaart sluit de overlay NIET.
 * - De terugknop van Android sluit de overlay NIET.
 * - Alleen de grote knop "Begrepen" sluit de melding en stopt de 5-minuten herhaallus.
 * - Met "Opnieuw voorlezen" wordt de Nederlandse melding direct nogmaals uitgesproken.
 */
@Composable
fun ReminderOverlay(
    reminder: ActiveReminder,
    now: LocalDateTime,
    onReadAgainClick: () -> Unit,
    onUnderstoodClick: () -> Unit,
) {
    // Blokkeer de systeem-terugknop zolang de herinnering zichtbaar is
    BackHandler(enabled = true) {
        // Bewust leeg: mag uitsluitend via "Begrepen" worden gesloten.
    }

    val appointment = reminder.appointment
    val timeRange = DutchDateTimeFormatter.formatAppointmentTimeLabel(appointment)
    val countdownLabel = DutchSpeechScriptBuilder.formatCountdownBadge(appointment, now)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgendaColors.ReminderBackdrop)
            .pointerInput(Unit) {
                // Vang alle tikken op de achtergrond af zonder de overlay te sluiten
                detectTapGestures { /* Bewust niet sluiten */ }
            },
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(40.dp)
                .widthIn(min = 640.dp, max = 920.dp),
            color = AgendaColors.ReminderCardBg,
            shape = RoundedCornerShape(30.dp),
            border = BorderStroke(4.dp, AgendaColors.ReminderAccent),
            shadowElevation = 24.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 44.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint = AgendaColors.ReminderAccent,
                        modifier = Modifier.size(40.dp),
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "HERINNERING VOOR VANDAAG",
                        color = AgendaColors.ReminderAccent,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.4.sp,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(AgendaColors.ReminderAccent.copy(alpha = 0.2f))
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = countdownLabel,
                        color = AgendaColors.AccentAmber,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(modifier = Modifier.height(26.dp))

                Text(
                    text = timeRange,
                    color = AgendaColors.AccentAmber,
                    fontSize = 54.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = appointment.title,
                    color = AgendaColors.TextPrimary,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 56.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = "Wordt elke 5 minuten herhaald totdat u op Begrepen tikt",
                    color = AgendaColors.TextSecondary,
                    fontSize = 19.sp,
                    textAlign = TextAlign.Center,
                )

                Spacer(modifier = Modifier.height(34.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Button(
                        onClick = onReadAgainClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AgendaColors.AccentBlueButton,
                            contentColor = AgendaColors.TextPrimary,
                        ),
                        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Opnieuw voorlezen",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Button(
                        onClick = onUnderstoodClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AgendaColors.AccentGreen,
                            contentColor = AgendaColors.AccentGreenOn,
                        ),
                        contentPadding = PaddingValues(vertical = 24.dp, horizontal = 20.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Begrepen",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
    }
}
