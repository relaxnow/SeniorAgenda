package nl.senioragenda.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import java.time.LocalTime

enum class DisplayThemeMode(val storageKey: String) {
    DARK("dark"),
    LIGHT("light"),
    AUTO("auto");

    companion object {
        fun fromStorageKey(key: String?): DisplayThemeMode {
            return entries.firstOrNull { it.storageKey == key } ?: DARK
        }
    }
}

object DisplayThemePolicy {
    val LIGHT_DAY_START: LocalTime = LocalTime.of(5, 30)
    val DARK_EVENING_START: LocalTime = LocalTime.of(19, 0)

    fun shouldUseLightTheme(mode: DisplayThemeMode, time: LocalTime): Boolean = when (mode) {
        DisplayThemeMode.DARK -> false
        DisplayThemeMode.LIGHT -> true
        DisplayThemeMode.AUTO -> !time.isBefore(LIGHT_DAY_START) && time.isBefore(DARK_EVENING_START)
    }
}

@Immutable
data class AgendaPalette(
    val background: Color,
    val surfacePanel: Color,
    val surfaceCard: Color,
    val surfaceCardHighlight: Color,
    val borderSubtle: Color,
    val borderHighlight: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accentAmber: Color,
    val accentAmberOn: Color,
    val accentSky: Color,
    val accentSkyOnBadge: Color,
    val accentSkyContainer: Color,
    val accentGreen: Color,
    val accentGreenOn: Color,
    val accentBlueButton: Color,
    val accentPurpleGemini: Color,
    val onColoredButton: Color,
    val reminderBackdrop: Color,
    val reminderCardBg: Color,
    val reminderAccent: Color,
    val reminderUrgent: Color,
    val nightRestBackground: Color,
    val nightRestText: Color,
    val nightRestHint: Color,
)

/**
 * Hoog-contrast donker kleurenpalet, rustig in de avond en zonder verblinding.
 */
val DarkAgendaPalette = AgendaPalette(
    background = Color(0xFF0D1117),
    surfacePanel = Color(0xFF151C26),
    surfaceCard = Color(0xFF1E293B),
    surfaceCardHighlight = Color(0xFF25354D),
    borderSubtle = Color(0xFF334155),
    borderHighlight = Color(0xFFFBBF24),
    textPrimary = Color(0xFFF8FAFC),
    textSecondary = Color(0xFFCBD5E1),
    textMuted = Color(0xFF94A3B8),
    accentAmber = Color(0xFFFBBF24),
    accentAmberOn = Color(0xFF111827),
    accentSky = Color(0xFF38BDF8),
    accentSkyOnBadge = Color(0xFF0D1117),
    accentSkyContainer = Color(0xFF0C2D42),
    accentGreen = Color(0xFF22C55E),
    accentGreenOn = Color(0xFF052E16),
    accentBlueButton = Color(0xFF2563EB),
    accentPurpleGemini = Color(0xFF7C3AED),
    onColoredButton = Color(0xFFF8FAFC),
    reminderBackdrop = Color(0xD905070B),
    reminderCardBg = Color(0xFF1A1F2C),
    reminderAccent = Color(0xFFF59E0B),
    reminderUrgent = Color(0xFFEF4444),
    nightRestBackground = Color(0xFF05070A),
    nightRestText = Color(0xFF334155),
    nightRestHint = Color(0xFF243040),
)

/**
 * Warm papieren licht kleurenpalet (zonder fel blauw-wit), geoptimaliseerd voor overdag
 * in lichte kamers en rustiger leesbaarheid bij astigmatisme of reflecties op glas.
 */
val LightAgendaPalette = AgendaPalette(
    background = Color(0xFFEBE7DD),
    surfacePanel = Color(0xFFFAF8F3),
    surfaceCard = Color(0xFFF0ECE2),
    surfaceCardHighlight = Color(0xFFFEF3C7),
    borderSubtle = Color(0xFFC8C2B4),
    borderHighlight = Color(0xFFB45309),
    textPrimary = Color(0xFF0F172A),
    textSecondary = Color(0xFF334155),
    textMuted = Color(0xFF64748B),
    accentAmber = Color(0xFFB45309),
    accentAmberOn = Color(0xFFFFFFFF),
    accentSky = Color(0xFF0369A1),
    accentSkyOnBadge = Color(0xFFFFFFFF),
    accentSkyContainer = Color(0xFFE0F2FE),
    accentGreen = Color(0xFF15803D),
    accentGreenOn = Color(0xFFFFFFFF),
    accentBlueButton = Color(0xFF1D4ED8),
    accentPurpleGemini = Color(0xFF6D28D9),
    onColoredButton = Color(0xFFFFFFFF),
    reminderBackdrop = Color(0xCC0F172A),
    reminderCardBg = Color(0xFFFFFBEB),
    reminderAccent = Color(0xFFB45309),
    reminderUrgent = Color(0xFFDC2626),
    nightRestBackground = Color(0xFF05070A),
    nightRestText = Color(0xFF334155),
    nightRestHint = Color(0xFF243040),
)

val LocalAgendaColors = staticCompositionLocalOf { DarkAgendaPalette }

object AgendaTheme {
    val colors: AgendaPalette
        @Composable
        get() = LocalAgendaColors.current
}

private val SeniorDarkColorScheme = darkColorScheme(
    primary = DarkAgendaPalette.accentAmber,
    onPrimary = DarkAgendaPalette.accentAmberOn,
    secondary = DarkAgendaPalette.accentSky,
    onSecondary = Color(0xFF082F49),
    background = DarkAgendaPalette.background,
    onBackground = DarkAgendaPalette.textPrimary,
    surface = DarkAgendaPalette.surfacePanel,
    onSurface = DarkAgendaPalette.textPrimary,
    surfaceVariant = DarkAgendaPalette.surfaceCard,
    onSurfaceVariant = DarkAgendaPalette.textSecondary,
    outline = DarkAgendaPalette.borderSubtle,
)

private val SeniorLightColorScheme = lightColorScheme(
    primary = LightAgendaPalette.accentAmber,
    onPrimary = LightAgendaPalette.accentAmberOn,
    secondary = LightAgendaPalette.accentSky,
    onSecondary = Color.White,
    background = LightAgendaPalette.background,
    onBackground = LightAgendaPalette.textPrimary,
    surface = LightAgendaPalette.surfacePanel,
    onSurface = LightAgendaPalette.textPrimary,
    surfaceVariant = LightAgendaPalette.surfaceCard,
    onSurfaceVariant = LightAgendaPalette.textSecondary,
    outline = LightAgendaPalette.borderSubtle,
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
    useLightTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    val palette = if (useLightTheme) LightAgendaPalette else DarkAgendaPalette
    val colorScheme = if (useLightTheme) SeniorLightColorScheme else SeniorDarkColorScheme

    CompositionLocalProvider(LocalAgendaColors provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = SeniorTypography,
            content = content,
        )
    }
}
