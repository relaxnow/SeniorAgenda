package nl.senioragenda.app.ui.theme

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalTime

class DisplayThemePolicyTest {

    @Test
    fun darkModeAlwaysUsesDarkPalette() {
        assertFalse(DisplayThemePolicy.shouldUseLightTheme(DisplayThemeMode.DARK, LocalTime.of(12, 0)))
        assertFalse(DisplayThemePolicy.shouldUseLightTheme(DisplayThemeMode.DARK, LocalTime.of(22, 0)))
    }

    @Test
    fun lightModeAlwaysUsesLightPalette() {
        assertTrue(DisplayThemePolicy.shouldUseLightTheme(DisplayThemeMode.LIGHT, LocalTime.of(12, 0)))
        assertTrue(DisplayThemePolicy.shouldUseLightTheme(DisplayThemeMode.LIGHT, LocalTime.of(22, 0)))
    }

    @Test
    fun autoModeUsesLightFrom0530To1900AndDarkInEvening() {
        assertFalse(DisplayThemePolicy.shouldUseLightTheme(DisplayThemeMode.AUTO, LocalTime.of(5, 29)))
        assertTrue(DisplayThemePolicy.shouldUseLightTheme(DisplayThemeMode.AUTO, LocalTime.of(5, 30)))
        assertTrue(DisplayThemePolicy.shouldUseLightTheme(DisplayThemeMode.AUTO, LocalTime.of(18, 59)))
        assertFalse(DisplayThemePolicy.shouldUseLightTheme(DisplayThemeMode.AUTO, LocalTime.of(19, 0)))
        assertFalse(DisplayThemePolicy.shouldUseLightTheme(DisplayThemeMode.AUTO, LocalTime.of(23, 30)))
    }
}
