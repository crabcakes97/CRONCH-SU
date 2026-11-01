package me.weishu.kernelsu.ui.theme

import androidx.annotation.StringRes
import com.materialkolor.PaletteStyle
import me.weishu.kernelsu.R

/**
 * Cronch SU theme presets: the ten flavors of the CRONCH archive, each in a
 * Day (light) and an After Dark (dark) variant — 20 presets total.
 *
 * A preset is only data: a Material seed color, a palette style, and a color
 * mode. The seed is fed into the same Material dynamic-color scheme the app
 * already uses everywhere (rememberKernelSUColorScheme / Miuix
 * ThemeController), so every surface — backgrounds, cards, accents, text,
 * buttons, toggles, status indicators — follows the active theme, and text
 * contrast is guaranteed by the scheme generator, never by hand-picked pairs.
 * Nothing here touches the root mechanism.
 */
data class CronchThemePreset(
    val id: String,
    @StringRes val nameRes: Int,
    /** ARGB seed color, alpha always 0xFF. 0 is never used here (0 means
     *  "system dynamic color" in the existing keyColor pref). */
    val seedArgb: Int,
    val paletteStyle: PaletteStyle,
    val colorMode: ColorMode,
) {
    val isDark: Boolean get() = colorMode.isDark || colorMode.isAmoled
}

val CRONCH_THEMES: List<CronchThemePreset> = listOf(
    // Original — the founding flavor. Dorito orange.
    CronchThemePreset("original", R.string.cronch_theme_original, 0xFFE8590C.toInt(), PaletteStyle.TonalSpot, ColorMode.LIGHT),
    CronchThemePreset("original_dark", R.string.cronch_theme_original_dark, 0xFFE8590C.toInt(), PaletteStyle.TonalSpot, ColorMode.DARK),
    // Alpine Ranch — cool mountain ranch blue.
    CronchThemePreset("alpine", R.string.cronch_theme_alpine, 0xFF3D7EA6.toInt(), PaletteStyle.Neutral, ColorMode.LIGHT),
    CronchThemePreset("alpine_dark", R.string.cronch_theme_alpine_dark, 0xFF3D7EA6.toInt(), PaletteStyle.Neutral, ColorMode.DARK),
    // Louisiana Crab Gumbo — crustacean red.
    CronchThemePreset("gumbo", R.string.cronch_theme_gumbo, 0xFFC1272D.toInt(), PaletteStyle.Vibrant, ColorMode.LIGHT),
    CronchThemePreset("gumbo_dark", R.string.cronch_theme_gumbo_dark, 0xFFC1272D.toInt(), PaletteStyle.Vibrant, ColorMode.DARK),
    // Rotisserie Chicken — golden roast amber.
    CronchThemePreset("rotisserie", R.string.cronch_theme_rotisserie, 0xFFB97E10.toInt(), PaletteStyle.Expressive, ColorMode.LIGHT),
    CronchThemePreset("rotisserie_dark", R.string.cronch_theme_rotisserie_dark, 0xFFB97E10.toInt(), PaletteStyle.Expressive, ColorMode.DARK),
    // Nuclear Ranch — emergency flavor. Radioactive green, AMOLED dark.
    CronchThemePreset("nuclear", R.string.cronch_theme_nuclear, 0xFF4C9A1F.toInt(), PaletteStyle.Vibrant, ColorMode.LIGHT),
    CronchThemePreset("nuclear_dark", R.string.cronch_theme_nuclear_dark, 0xFF7CFC3A.toInt(), PaletteStyle.Vibrant, ColorMode.DARK_AMOLED),
    // Nacho Rebellion — cheese gold.
    CronchThemePreset("rebellion", R.string.cronch_theme_rebellion, 0xFFD98E00.toInt(), PaletteStyle.TonalSpot, ColorMode.LIGHT),
    CronchThemePreset("rebellion_dark", R.string.cronch_theme_rebellion_dark, 0xFFD98E00.toInt(), PaletteStyle.TonalSpot, ColorMode.DARK),
    // Nacho Ranch — the truce blend. Warm cream-orange.
    CronchThemePreset("nachoranch", R.string.cronch_theme_nachoranch, 0xFFC46A1B.toInt(), PaletteStyle.Neutral, ColorMode.LIGHT),
    CronchThemePreset("nachoranch_dark", R.string.cronch_theme_nachoranch_dark, 0xFFC46A1B.toInt(), PaletteStyle.Neutral, ColorMode.DARK),
    // Family Reserve — the good stuff. Deep plum.
    CronchThemePreset("reserve", R.string.cronch_theme_reserve, 0xFF6D4C93.toInt(), PaletteStyle.Expressive, ColorMode.LIGHT),
    CronchThemePreset("reserve_dark", R.string.cronch_theme_reserve_dark, 0xFF6D4C93.toInt(), PaletteStyle.Expressive, ColorMode.DARK),
    // Emergency Stash — alert red, AMOLED dark.
    CronchThemePreset("stash", R.string.cronch_theme_stash, 0xFFB3261E.toInt(), PaletteStyle.Vibrant, ColorMode.LIGHT),
    CronchThemePreset("stash_dark", R.string.cronch_theme_stash_dark, 0xFFFF6B5E.toInt(), PaletteStyle.Vibrant, ColorMode.DARK_AMOLED),
    // Mystery CRONCH — nobody knows. Teal.
    CronchThemePreset("mystery", R.string.cronch_theme_mystery, 0xFF0F7B6C.toInt(), PaletteStyle.Expressive, ColorMode.LIGHT),
    CronchThemePreset("mystery_dark", R.string.cronch_theme_mystery_dark, 0xFF2EC4A6.toInt(), PaletteStyle.Expressive, ColorMode.DARK_AMOLED),
)

/** The preset id recorded when the user picks a hand-made accent color. */
const val CRONCH_THEME_CUSTOM_ID = "custom"

fun cronchThemeById(id: String): CronchThemePreset? =
    CRONCH_THEMES.firstOrNull { it.id == id }
