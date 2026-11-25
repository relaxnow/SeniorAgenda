package nl.senioragenda.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.senioragenda.app.domain.formatting.DutchDateTimeFormatter
import nl.senioragenda.app.speech.SpeechStatus
import nl.senioragenda.app.speech.SpeechUiState
import nl.senioragenda.app.ui.CalendarSyncStatus
import nl.senioragenda.app.ui.theme.AgendaColors
import java.time.LocalDateTime

@Composable
fun TopDashboardBar(
    now: LocalDateTime,
    syncStatus: CalendarSyncStatus,
    syncStatusLabel: String,
    speechState: SpeechUiState,
    onSpeakClick: () -> Unit,
    onAddAppointmentClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AgendaColors.SurfacePanel,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.5.dp, AgendaColors.BorderSubtle),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Linkerzijde: Nederlandse dag & datum + subtiele statuschip voor familiebeheer
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = DutchDateTimeFormatter.formatTopBarDate(now.toLocalDate()),
                    color = AgendaColors.TextPrimary,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AgendaColors.SurfaceCard)
                            .clickable(onClick = onSettingsClick)
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (syncStatus == CalendarSyncStatus.CONNECTED) {
                                Icons.Rounded.CloudDone
                            } else {
                                Icons.Rounded.Settings
                            },
                            contentDescription = "Agenda-instellingen",
                            tint = if (syncStatus == CalendarSyncStatus.CONNECTED) {
                                AgendaColors.AccentGreen
                            } else {
                                AgendaColors.TextMuted
                            },
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = syncStatusLabel,
                            color = AgendaColors.TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    if (speechState.status == SpeechStatus.DUTCH_VOICE_MISSING) {
                        Text(
                            text = "Nederlandse spraakstem ontbreekt",
                            color = AgendaColors.ReminderAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Midden-rechts: Grote duidelijke knoppen "Spreek voor" en "Afspraak toevoegen"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Button(
                    onClick = onSpeakClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (speechState.isSpeaking) {
                            AgendaColors.ReminderUrgent
                        } else {
                            AgendaColors.AccentBlueButton
                        },
                        contentColor = AgendaColors.TextPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
                ) {
                    Icon(
                        imageVector = if (speechState.isSpeaking) {
                            Icons.Rounded.Stop
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = null,
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (speechState.isSpeaking) "Stop voorlezen" else "Spreek voor",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Button(
                    onClick = onAddAppointmentClick,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AgendaColors.AccentPurpleGemini,
                        contentColor = AgendaColors.TextPrimary,
                    ),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 16.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Mic,
                        contentDescription = null,
                        modifier = Modifier.size(26.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = AgendaColors.AccentAmber,
                        modifier = Modifier.size(22.dp),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Afspraak toevoegen",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(modifier = Modifier.width(24.dp))

            // Uiterst rechts: Grote 24-uurs klok
            Text(
                text = DutchDateTimeFormatter.formatTime(now.toLocalTime()),
                color = AgendaColors.AccentAmber,
                fontSize = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
            )
        }
    }
}
