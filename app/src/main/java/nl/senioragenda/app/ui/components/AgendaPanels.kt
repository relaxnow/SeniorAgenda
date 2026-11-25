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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.senioragenda.app.domain.formatting.DutchDateTimeFormatter
import nl.senioragenda.app.domain.model.Appointment
import nl.senioragenda.app.domain.model.DayAgenda
import nl.senioragenda.app.ui.theme.AgendaTheme
import java.time.LocalTime

@Composable
fun TodayPanel(
    todayAgenda: DayAgenda,
    currentTime: LocalTime,
    modifier: Modifier = Modifier,
) {
    val colors = AgendaTheme.colors

    Surface(
        modifier = modifier.fillMaxHeight(),
        color = colors.surfacePanel,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
        ) {
            SectionHeader(
                title = "VANDAAG",
                accentColor = colors.accentAmber,
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
                            color = colors.textPrimary,
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Een rustige dag",
                            color = colors.textSecondary,
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
    val colors = AgendaTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = colors.accentSkyContainer,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(2.dp, colors.accentSky),
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
                    .background(colors.accentSky)
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text(
                    text = "HELE DAG",
                    color = colors.accentSkyOnBadge,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.8.sp,
                )
            }
            Spacer(modifier = Modifier.width(18.dp))
            Text(
                text = appointment.title,
                color = colors.textPrimary,
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
    val colors = AgendaTheme.colors
    val backgroundColor = if (isHighlighted) colors.surfaceCardHighlight else colors.surfaceCard
    val borderColor = if (isHighlighted) colors.borderHighlight else colors.borderSubtle
    val timeColor = if (isPast) colors.textMuted else colors.accentAmber
    val titleColor = if (isPast) colors.textMuted else colors.textPrimary

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
    val colors = AgendaTheme.colors

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surfacePanel,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            SectionHeader(
                title = DutchDateTimeFormatter.formatTomorrowHeader(tomorrowAgenda.date),
                accentColor = colors.accentSky,
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (tomorrowAgenda.isEmpty) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Text(
                        text = "Geen afspraken morgen",
                        color = colors.textSecondary,
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
    val colors = AgendaTheme.colors

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = if (appointment.isAllDay) colors.accentSkyContainer else colors.surfaceCard,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (appointment.isAllDay) colors.accentSky else colors.borderSubtle,
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
                color = if (appointment.isAllDay) colors.accentSky else colors.accentAmber,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.width(152.dp),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = appointment.title,
                color = colors.textPrimary,
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
    val colors = AgendaTheme.colors

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = colors.surfacePanel,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, colors.borderSubtle),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
        ) {
            SectionHeader(
                title = "DAARNA",
                accentColor = colors.textSecondary,
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
                            color = colors.borderSubtle.copy(alpha = 0.65f),
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
    val colors = AgendaTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = DutchDateTimeFormatter.formatCompactDay(dayAgenda.date),
            color = colors.textPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(118.dp),
        )

        Spacer(modifier = Modifier.width(10.dp))

        if (dayAgenda.isEmpty) {
            Text(
                text = "Geen afspraken",
                color = colors.textMuted,
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
                            color = if (appointment.isAllDay) colors.accentSky else colors.accentAmber,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(136.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = appointment.title,
                            color = colors.textPrimary,
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
    accentColor: Color,
) {
    val colors = AgendaTheme.colors

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
            color = colors.textPrimary,
            fontSize = 23.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
