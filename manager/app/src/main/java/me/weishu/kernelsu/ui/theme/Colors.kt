package me.weishu.kernelsu.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp

val keyColorOptions = listOf(
    Color(0xFFF44336).toArgb(),
    Color(0xFFE91E63).toArgb(),
    Color(0xFF9C27B0).toArgb(),
    Color(0xFF673AB7).toArgb(),
    Color(0xFF3F51B5).toArgb(),
    Color(0xFF2196F3).toArgb(),
    Color(0xFF00BCD4).toArgb(),
    Color(0xFF009688).toArgb(),
    Color(0xFF4FAF50).toArgb(),
    Color(0xFFFFEB3B).toArgb(),
    Color(0xFFFFC107).toArgb(),
    Color(0xFFFF9800).toArgb(),
    Color(0xFF795548).toArgb(),
    Color(0xFF607D8F).toArgb(),
    Color(0xFFFF9CA8).toArgb(),
)

/**
 * The CRONCH palette. No grey card surfaces anywhere: backgrounds are
 * ranch cream (light) or charred coop brown (dark), accents are the ten
 * archive flavors. Every card in the app is painted from these colors,
 * never from a stock grey container.
 */
val CronchDoritoOrange = Color(0xFFE8590C)
val CronchRanchCream = Color(0xFFFFF6E8)
val CronchRanchCreamDark = Color(0xFF2A1A0C)
val CronchCoopBrown = Color(0xFF3B2314)
val CronchMoaiStone = Color(0xFF8A7B6B)
val CronchSlayPink = Color(0xFFE84A8A)
val CronchGumboRed = Color(0xFFC1272D)
val CronchNuclearGreen = Color(0xFF4C9A1F)
val CronchMysteryTeal = Color(0xFF0F7B6C)
val CronchNachoGold = Color(0xFFD98E00)
val CronchRotisserieAmber = Color(0xFFB97E10)
val CronchAlpineBlue = Color(0xFF3D7EA6)
val CronchReservePlum = Color(0xFF6D4C93)
val CronchStashRed = Color(0xFFB3261E)
val CronchLogDarkSurface = Color(0xFF241407)
val CronchLogDarkText = Color(0xFFFFF1D6)
val CronchLogLightSurface = Color(0xFFFFF3DC)
val CronchLogLightText = Color(0xFF3B2314)

/** Ranch-cream hero panel color in either theme: cream in the light coop,
 *  warm charred brown in the dark coop, so the panel never reads grey. */
fun cronchPanelColor(isDark: Boolean): Color =
    if (isDark) CronchRanchCreamDark else CronchRanchCream

/** Text color that stays readable on the ranch-cream panel. */
fun cronchPanelContentColor(isDark: Boolean): Color =
    if (isDark) CronchRanchCream else CronchCoopBrown

/** Coop Log surface and text for either theme. */
fun cronchLogSurfaceColor(isDark: Boolean): Color =
    if (isDark) CronchLogDarkSurface else CronchLogLightSurface

fun cronchLogContentColor(isDark: Boolean): Color =
    if (isDark) CronchLogDarkText else CronchLogLightText

/** The ten archive flavors, in archive order, for the wordmark strip. */
val CronchFlavorStripColors = listOf(
    CronchDoritoOrange,
    CronchAlpineBlue,
    CronchGumboRed,
    CronchRotisserieAmber,
    CronchNuclearGreen,
    CronchNachoGold,
    Color(0xFFC46A1B),
    CronchReservePlum,
    CronchStashRed,
    CronchMysteryTeal,
)

/** CRONCH shapes: big soft card corners, full pill for tags and strips. */
val CronchCardShape = RoundedCornerShape(20.dp)
val CronchLogShape = RoundedCornerShape(16.dp)
val CronchPillShape = RoundedCornerShape(percent = 50)
