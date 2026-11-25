package nl.senioragenda.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.senioragenda.app.domain.formatting.DutchDateTimeFormatter
import nl.senioragenda.app.ui.theme.AgendaColors
import java.time.LocalDateTime

/**
 * Zeer donker rustscherm voor de nachtstand (23:30 - 05:30) in combinatie met minimale
 * LCD-achtergrondverlichting (0.01f) voor de Samsung Galaxy Tab S9 FE.
 * Tikken op het scherm wekt het dashboard tijdelijk voor 30 seconden.
 */
@Composable
fun NightModeRestOverlay(
    now: LocalDateTime,
    isManualPreview: Boolean,
    onTapToWake: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AgendaColors.NightRestBackground)
            .pointerInput(Unit) {
                detectTapGestures { onTapToWake() }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = DutchDateTimeFormatter.formatTime(now.toLocalTime()),
                color = AgendaColors.NightRestText,
                fontSize = 84.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (isManualPreview) {
                    "Nachtstand (voorbeeld) — tik op het scherm om te kijken"
                } else {
                    "Nachtstand — tik op het scherm om even te kijken"
                },
                color = AgendaColors.NightRestHint,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun TemporaryWakeBanner(
    secondsRemaining: Long,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        color = AgendaColors.SurfaceCard,
        shape = RoundedCornerShape(12.dp),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Nachtstand tijdelijk onderbroken — dimt over $secondsRemaining sec automatisch weer",
                color = AgendaColors.AccentAmber,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}
