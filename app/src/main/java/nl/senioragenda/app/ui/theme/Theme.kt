package nl.senioragenda.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Hoog-contrast donker kleurenpalet, geoptimaliseerd voor het 10,9" TFT LCD-scherm
 * van de Samsung Galaxy Tab S9 FE (rustig voor de ogen, leesbaar vanaf de andere kant van de kamer).
 */
object AgendaColors {
    val Background = Color(0xFF0D1117)
    val SurfacePanel = Color(0xFF151C26)
    val SurfaceCard = Color(0xFF1E293B)
    val SurfaceCardHighlight = Color(0xFF25354D)
    val BorderSubtle = Color(0xFF334155)
    val BorderHighlight = Color(0xFFFBBF24)

    val TextPrimary = Color(0xFFF8FAFC)
    val TextSecondary = Color(0xFFCBD5E1)
    val TextMuted = Color(0xFF94A3B8)

    val AccentAmber = Color(0xFFFBBF24)
    val AccentAmberOn = Color(0xFF111827)

    val AccentSky = Color(0xFF38BDF8)
    val AccentSkyContainer = Color(0xFF0C2D42)

    val AccentGreen = Color(0xFF22C55E)
    val AccentGreenOn = Color(0xFF052E16)

    val AccentBlueButton = Color(0xFF2563EB)
    val AccentPurpleGemini = Color(0xFF7C3AED)

    val ReminderBackdrop = Color(0xD905070B)
    val ReminderCardBg = Color(0xFF1A1F2C)
    val ReminderAccent = Color(0xFFF59E0B)
    val ReminderUrgent = Color(0xFFEF4444)

    val NightRestBackground = Color(0xFF05070A)
    val NightRestText = Color(0xFF334155)
    val NightRestHint = Color(0xFF243040)
}

private val SeniorDarkColorScheme = darkColorScheme(
    primary = AgendaColors.AccentAmber,
    onPrimary = AgendaColors.AccentAmberOn,
    secondary = AgendaColors.AccentSky,
    onSecondary = Color(0xFF082F49),
    background = AgendaColors.Background,
    onBackground = AgendaColors.TextPrimary,
    surface = AgendaColors.SurfacePanel,
    onSurface = AgendaColors.TextPrimary,
    surfaceVariant = AgendaColors.SurfaceCard,
    onSurfaceVariant = AgendaColors.TextSecondary,
    outline = AgendaColors.BorderSubtle,
)

private val SeniorTypography = Typography(
    displayLarge = TextStyle(
        fontSize = 48.sp,
        lineHeight = 54.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineLarge = TextStyle(
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold,
    ),
    headlineMedium = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleLarge = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
    ),
    titleMedium = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 17.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 18.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
    ),
)

@Composable
fun SeniorAgendaTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = SeniorDarkColorScheme,
        typography = SeniorTypography,
        content = content,
    )
}
