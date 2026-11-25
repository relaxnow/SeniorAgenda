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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import nl.senioragenda.app.domain.formatting.DutchDateTimeFormatter
import nl.senioragenda.app.speech.VoiceInputState
import nl.senioragenda.app.ui.AddAppointmentSheetState
import nl.senioragenda.app.ui.theme.AgendaTheme

@Composable
fun AddAppointmentModal(
    state: AddAppointmentSheetState,
    voiceState: VoiceInputState,
    isGeminiConfigured: Boolean,
    onInputChanged: (String) -> Unit,
    onStartVoiceClick: () -> Unit,
    onStopVoiceClick: () -> Unit,
    onParseClick: () -> Unit,
    onConfirmSaveClick: () -> Unit,
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
                .padding(32.dp)
                .widthIn(min = 660.dp, max = 900.dp),
            color = colors.surfacePanel,
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(2.dp, colors.accentPurpleGemini),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = colors.accentAmber,
                            modifier = Modifier.size(30.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Afspraak toevoegen",
                            color = colors.textPrimary,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                        )
                    }

                    OutlinedButton(
                        onClick = onDismissClick,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.5.dp, colors.borderSubtle),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = null,
                            tint = colors.textPrimary,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sluiten",
                            color = colors.textPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (isGeminiConfigured) {
                        "Spreek of typ in gewoon Nederlands. Gemini zet dit om naar datum, begintijd en eindtijd (standaard 1 uur)."
                    } else {
                        "Spreek of typ in gewoon Nederlands, bijvoorbeeld: \"Morgen om half drie tandarts\" of \"Zaterdag hele dag hapjes komen aan\"."
                    },
                    color = colors.textSecondary,
                    fontSize = 18.sp,
                )

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Button(
                        onClick = if (voiceState.isListening) onStopVoiceClick else onStartVoiceClick,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (voiceState.isListening) {
                                colors.reminderUrgent
                            } else {
                                colors.accentPurpleGemini
                            },
                            contentColor = colors.onColoredButton,
                        ),
                        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 18.dp),
                    ) {
                        Icon(
                            imageVector = if (voiceState.isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = if (voiceState.isListening) "Stop luisteren" else "Spreek in",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = onInputChanged,
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Bijv. Morgen van 14:00 tot 15:00 huisarts",
                                color = colors.textMuted,
                                fontSize = 19.sp,
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = colors.textPrimary,
                            fontSize = 21.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.accentAmber,
                            unfocusedBorderColor = colors.borderSubtle,
                            focusedContainerColor = colors.surfaceCard,
                            unfocusedContainerColor = colors.surfaceCard,
                        ),
                    )

                    Button(
                        onClick = onParseClick,
                        enabled = !state.isParsing && state.inputText.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accentBlueButton,
                            contentColor = colors.onColoredButton,
                        ),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp),
                    ) {
                        if (state.isParsing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = colors.onColoredButton,
                                strokeWidth = 3.dp,
                            )
                        } else {
                            Text(
                                text = "Controleer",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                if (voiceState.isListening) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Ik luister in het Nederlands… Spreek uw afspraak rustig in.",
                        color = colors.accentAmber,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                if (!state.feedbackMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = state.feedbackMessage,
                        color = colors.reminderAccent,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                state.parsedDraft?.let { draft ->
                    Spacer(modifier = Modifier.height(22.dp))
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = colors.surfaceCardHighlight,
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(2.dp, colors.accentGreen),
                    ) {
                        Column(modifier = Modifier.padding(22.dp)) {
                            Text(
                                text = "VOORBEELD VAN NIEUWE AFSPRAAK",
                                color = colors.accentGreen,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp,
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = DutchDateTimeFormatter.formatTopBarDate(draft.date),
                                color = colors.textSecondary,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (draft.isAllDay) {
                                    "HELE DAG - ${draft.title}"
                                } else {
                                    val range = DutchDateTimeFormatter.formatTimeRange(
                                        requireNotNull(draft.startTime),
                                        requireNotNull(draft.endTime),
                                    )
                                    "$range  •  ${draft.title}"
                                },
                                color = colors.textPrimary,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                            )
                            Spacer(modifier = Modifier.height(18.dp))
                            Button(
                                onClick = onConfirmSaveClick,
                                enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.accentGreen,
                                    contentColor = colors.accentGreenOn,
                                ),
                                contentPadding = PaddingValues(vertical = 18.dp),
                            ) {
                                if (state.isSaving) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(26.dp),
                                        color = colors.accentGreenOn,
                                        strokeWidth = 3.dp,
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Opslaan in agenda",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
