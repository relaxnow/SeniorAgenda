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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import nl.senioragenda.app.ui.theme.AgendaTheme
import nl.senioragenda.app.ui.theme.DisplayThemeMode

/**
 * Beheer- en testvenster voor familieleden (verborgen achter het kleine statuslabel in de kopbalk
 * zodat het hoofdscherm voor vader volledig rustig en vrij van menu's blijft).
 */
@Composable
fun FamilySettingsModal(
    state: FamilySettingsState,
    manualNightPreview: Boolean,
    selectedThemeMode: DisplayThemeMode,
    onThemeModeSelected: (DisplayThemeMode) -> Unit,
    onCalendarIdChanged: (String) -> Unit,
    onSaveCalendarId: () -> Unit,
    onSyncDeviceCalendarClick: () -> Unit,
    onConnectGoogleClick: () -> Unit,
    onTestReminderClick: () -> Unit,
    onToggleNightPreviewClick: () -> Unit,
    onDismissClick: () -> Unit,
) {
    val colors = AgendaTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.reminderBackdrop),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier
                .padding(28.dp)
                .widthIn(min = 660.dp, max = 860.dp),
            color = colors.surfacePanel,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, colors.borderSubtle),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(28.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Beheer & Google Agenda",
                        color = colors.textPrimary,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    OutlinedButton(
                        onClick = onDismissClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.5.dp, colors.borderSubtle),
                    ) {
                        Text(
                            text = "Sluiten",
                            color = colors.textPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Thema-keuze: Donker / Licht / Automatisch
                Text(
                    text = "Weergave (thema)",
                    color = colors.textPrimary,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Automatisch gebruikt warm licht van 05:30 tot 19:00, rustig donker in de avond, en nachtstand van 23:30 tot 05:30.",
                    color = colors.textSecondary,
                    fontSize = 15.sp,
                )
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ThemeModeOptionButton(
                        label = "Donker",
                        selected = selectedThemeMode == DisplayThemeMode.DARK,
                        onClick = { onThemeModeSelected(DisplayThemeMode.DARK) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeModeOptionButton(
                        label = "Licht (warm)",
                        selected = selectedThemeMode == DisplayThemeMode.LIGHT,
                        onClick = { onThemeModeSelected(DisplayThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f),
                    )
                    ThemeModeOptionButton(
                        label = "Automatisch",
                        selected = selectedThemeMode == DisplayThemeMode.AUTO,
                        onClick = { onThemeModeSelected(DisplayThemeMode.AUTO) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = colors.borderSubtle)
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Koppel een gedeelde Google Agenda zodat familieleden op afstand afspraken kunnen beheren (ook via Gemini op hun eigen telefoon).",
                    color = colors.textSecondary,
                    fontSize = 16.sp,
                )

                Spacer(modifier = Modifier.height(14.dp))

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
                                color = colors.textSecondary,
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                        ),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentAmber,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = colors.surfaceCard,
                            unfocusedContainerColor = colors.surfaceCard,
                        ),
                    )

                    Button(
                        onClick = onSaveCalendarId,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.surfaceCardHighlight,
                            contentColor = colors.textPrimary,
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
                    ) {
                        Text(text = "Bewaar ID", fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Button(
                        onClick = onSyncDeviceCalendarClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentGreen,
                            contentColor = colors.accentGreenOn,
                        ),
                        contentPadding = PaddingValues(vertical = 15.dp),
                    ) {
                        Text(
                            text = "Gebruik tablet Google Agenda",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    Button(
                        onClick = onConnectGoogleClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentBlueButton,
                            contentColor = colors.onColoredButton,
                        ),
                        contentPadding = PaddingValues(vertical = 15.dp),
                    ) {
                        Text(
                            text = "Koppel via Google Cloud OAuth",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Button(
                        onClick = onTestReminderClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.reminderAccent,
                            contentColor = colors.accentAmberOn,
                        ),
                        contentPadding = PaddingValues(vertical = 14.dp),
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
                        border = BorderStroke(1.5.dp, colors.accentSky),
                        contentPadding = PaddingValues(vertical = 14.dp),
                    ) {
                        Text(
                            text = if (manualNightPreview) "Stop nachtstand-voorbeeld" else "Test nachtstand (LCD)",
                            color = colors.accentSky,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (!state.statusNotice.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = state.statusNotice,
                        color = colors.accentAmber,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeOptionButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors

    if (selected) {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.accentAmber,
                contentColor = colors.accentAmberOn,
            ),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Text(
                text = label,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.5.dp, colors.borderSubtle),
            contentPadding = PaddingValues(vertical = 14.dp),
        ) {
            Text(
                text = label,
                color = colors.textPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
