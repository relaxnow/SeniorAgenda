package nl.senioragenda.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
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
import nl.senioragenda.app.domain.model.Appointment
import nl.senioragenda.app.domain.model.DayAgenda
import nl.senioragenda.app.ui.theme.AgendaColors
import java.time.LocalTime

@Composable
fun TodayPanel(
    todayAgenda: DayAgenda,
    currentTime: LocalTime,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxHeight(),
        color = AgendaColors.SurfacePanel,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, AgendaColors.BorderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            SectionHeader(
                title = "VANDAAG",
                accentColor = AgendaColors.AccentAmber,
            )

            Spacer(modifier = Modifier.height(18.dp))

            if (todayAgenda.isEmpty) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Geen afspraken vandaag",
                            color = AgendaColors.TextPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Een rustige dag",
                            color = AgendaColors.TextSecondary,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            } else {
                val nextTimedId = todayAgenda.timedItems
                    .firstOrNull { item ->
                        val end = item.endTime ?: LocalTime.MAX
                        !end.isBefore(currentTime)
                    }
                    ?.id

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    todayAgenda.allDayItems.forEach { item ->
                        TodayAllDayCard(appointment = item)
                    }
                    todayAgenda.timedItems.forEach { item ->
                        val isPast = item.endTime?.isBefore(currentTime) == true
                        val isHighlighted = item.id == nextTimedId
                        TodayTimedCard(
                            appointment = item,
                            isHighlighted = isHighlighted,
                            isPast = isPast,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TodayAllDayCard(appointment: Appointment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AgendaColors.AccentSkyContainer,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, AgendaColors.AccentSky),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(AgendaColors.AccentSky)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "HELE DAG",
                    color = AgendaColors.Background,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = appointment.title,
                color = AgendaColors.TextPrimary,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun TodayTimedCard(
    appointment: Appointment,
    isHighlighted: Boolean,
    isPast: Boolean,
) {
    val backgroundColor = when {
        isHighlighted -> AgendaColors.SurfaceCardHighlight
        else -> AgendaColors.SurfaceCard
    }
    val borderColor = when {
        isHighlighted -> AgendaColors.BorderHighlight
        else -> AgendaColors.BorderSubtle
    }
    val timeColor = when {
        isPast -> AgendaColors.TextMuted
        else -> AgendaColors.AccentAmber
    }
    val titleColor = when {
        isPast -> AgendaColors.TextMuted
        else -> AgendaColors.TextPrimary
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = backgroundColor,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(if (isHighlighted) 2.5.dp else 1.5.dp, borderColor),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
        ) {
            Text(
                text = DutchDateTimeFormatter.formatAppointmentTimeLabel(appointment),
                color = timeColor,
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.5.sp,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = appointment.title,
                color = titleColor,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 38.sp,
            )
        }
    }
}

@Composable
fun TomorrowPanel(
    tomorrowAgenda: DayAgenda,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AgendaColors.SurfacePanel,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, AgendaColors.BorderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            SectionHeader(
                title = DutchDateTimeFormatter.formatTomorrowHeader(tomorrowAgenda.date),
                accentColor = AgendaColors.AccentSky,
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (tomorrowAgenda.isEmpty) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "Geen afspraken morgen",
                        color = AgendaColors.TextSecondary,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    tomorrowAgenda.appointments.forEach { appointment ->
                        TomorrowAppointmentRow(appointment = appointment)
                    }
                }
            }
        }
    }
}

@Composable
private fun TomorrowAppointmentRow(appointment: Appointment) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (appointment.isAllDay) AgendaColors.AccentSkyContainer else AgendaColors.SurfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (appointment.isAllDay) AgendaColors.AccentSky else AgendaColors.BorderSubtle,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = DutchDateTimeFormatter.formatAppointmentTimeLabel(appointment),
                color = if (appointment.isAllDay) AgendaColors.AccentSky else AgendaColors.AccentAmber,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(152.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = appointment.title,
                color = AgendaColors.TextPrimary,
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun UpcomingDaysPanel(
    upcomingDays: List<DayAgenda>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AgendaColors.SurfacePanel,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, AgendaColors.BorderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            SectionHeader(
                title = "DAARNA",
                accentColor = AgendaColors.TextSecondary,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                upcomingDays.forEachIndexed { index, dayAgenda ->
                    UpcomingDayRow(dayAgenda = dayAgenda)
                    if (index < upcomingDays.lastIndex) {
                        HorizontalDivider(
                            color = AgendaColors.BorderSubtle.copy(alpha = 0.65f),
                            thickness = 1.dp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpcomingDayRow(dayAgenda: DayAgenda) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = DutchDateTimeFormatter.formatCompactDay(dayAgenda.date),
            color = AgendaColors.TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(118.dp),
        )

        Spacer(modifier = Modifier.width(10.dp))

        if (dayAgenda.isEmpty) {
            Text(
                text = "Geen afspraken",
                color = AgendaColors.TextMuted,
                fontSize = 19.sp,
                fontWeight = FontWeight.Normal,
            )
        } else {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                dayAgenda.appointments.forEach { appointment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (appointment.isAllDay) {
                                "HELE DAG"
                            } else {
                                DutchDateTimeFormatter.formatAppointmentTimeLabel(appointment)
                            },
                            color = if (appointment.isAllDay) {
                                AgendaColors.AccentSky
                            } else {
                                AgendaColors.AccentAmber
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(136.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = appointment.title,
                            color = AgendaColors.TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    accentColor: androidx.compose.ui.graphics.Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .width(7.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(accentColor),
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            color = AgendaColors.TextPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
