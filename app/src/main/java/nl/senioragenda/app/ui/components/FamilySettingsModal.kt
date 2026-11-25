package nl.senioragenda.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.senioragenda.app.ui.FamilySettingsState
import nl.senioragenda.app.ui.theme.AgendaColors

/**
 * Beheer- en testvenster voor familieleden (verborgen achter het kleine statuslabel in de kopbalk
 * zodat het hoofdscherm voor vader volledig rustig en vrij van menu's blijft).
 */
@Composable
fun FamilySettingsModal(
    state: FamilySettingsState,
    manualNightPreview: Boolean,
    onCalendarIdChanged: (String) -> Unit,
    onSaveCalendarId: () -> Unit,
    onConnectGoogleClick: () -> Unit,
    onTestReminderClick: () -> Unit,
    onToggleNightPreviewClick: () -> Unit,
    onDismissClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgendaColors.ReminderBackdrop),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(32.dp)
                .widthIn(min = 620.dp, max = 820.dp),
            color = AgendaColors.SurfacePanel,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, AgendaColors.BorderSubtle),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(28.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Beheer & Google Agenda",
                        color = AgendaColors.TextPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    OutlinedButton(
                        onClick = onDismissClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AgendaColors.BorderSubtle),
                    ) {
                        Text(
                            text = "Sluiten",
                            color = AgendaColors.TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Koppel een gedeelde Google Agenda zodat familieleden op afstand afspraken kunnen beheren (ook via Gemini op hun eigen telefoon).",
                    color = AgendaColors.TextSecondary,
                    fontSize = 17.sp,
                )

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = state.calendarIdInput,
                        onValueChange = onCalendarIdChanged,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = {
                            Text(
                                text = "Google Agenda-ID (standaard: primary)",
                                color = AgendaColors.TextSecondary,
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = AgendaColors.TextPrimary,
                            fontSize = 18.sp,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AgendaColors.AccentAmber,
                            unfocusedBorderColor = AgendaColors.BorderSubtle,
                            focusedContainerColor = AgendaColors.SurfaceCard,
                            unfocusedContainerColor = AgendaColors.SurfaceCard,
                        ),
                    )

                    Button(
                        onClick = onSaveCalendarId,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AgendaColors.SurfaceCardHighlight,
                            contentColor = AgendaColors.TextPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                    ) {
                        Text(text = "Bewaar ID", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Button(
                        onClick = onConnectGoogleClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AgendaColors.AccentBlueButton,
                            contentColor = AgendaColors.TextPrimary,
                        ),
                        contentPadding = PaddingValues(vertical = 16.dp),
                    ) {
                        Text(
                            text = "Koppel / Vernieuw Google Agenda",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Button(
                        onClick = onTestReminderClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AgendaColors.ReminderAccent,
                            contentColor = AgendaColors.AccentAmberOn,
                        ),
                        contentPadding = PaddingValues(vertical = 15.dp),
                    ) {
                        Text(
                            text = "Test herinnering (overlay + spraak)",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    OutlinedButton(
                        onClick = onToggleNightPreviewClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, AgendaColors.AccentSky),
                        contentPadding = PaddingValues(vertical = 15.dp),
                    ) {
                        Text(
                            text = if (manualNightPreview) "Stop nachtstand-voorbeeld" else "Test nachtstand (LCD)",
                            color = AgendaColors.AccentSky,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (!state.statusNotice.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = state.statusNotice,
                        color = AgendaColors.AccentAmber,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}
