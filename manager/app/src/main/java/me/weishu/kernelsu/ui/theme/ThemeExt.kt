package me.weishu.kernelsu.ui.theme

import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import com.materialkolor.rememberDynamicColorScheme

fun ColorScheme.amoledBackground(amoled: Boolean): ColorScheme =
    if (!amoled) this
    else copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceDim = Color.Black,
        surfaceContainerLowest = Color.Black,
        surfaceContainerLow = Color.Black,
        surfaceContainer = Color.Black,
        surfaceContainerHigh = Color.Black,
        surfaceContainerHighest = Color.Black,
    )

/** Linear blend of two opaque colors; t=0 is [a], t=1 is [b]. */
private fun cronchBlend(a: Color, b: Color, t: Float): Color =
    Color(
        red = a.red + (b.red - a.red) * t,
        green = a.green + (b.green - a.green) * t,
        blue = a.blue + (b.blue - a.blue) * t,
        alpha = 1f,
    )

/**
 * CRONCH-ify a generated scheme: backgrounds and surfaces become ranch
 * cream (light) or charred coop brown (dark), tinted toward the seed
 * color, so no surface in the app reads as stock grey. The seed-derived
 * primary/secondary/tertiary and all on-colors are left exactly as the
 * generator produced them, so the twenty flavor themes and their contrast
 * guarantees survive. Applied before the animation wrapper, which copies
 * every field through.
 */
fun ColorScheme.cronchified(isDark: Boolean): ColorScheme {
    val cream = CronchRanchCream
    val charred = CronchRanchCreamDark
    return if (isDark) {
        val bg = cronchBlend(charred, primary, 0.08f)
        copy(
            background = bg,
            surface = cronchBlend(charred, primary, 0.12f),
            surfaceVariant = cronchBlend(charred, primary, 0.18f),
            surfaceDim = cronchBlend(charred, primary, 0.05f),
            surfaceBright = cronchBlend(charred, primary, 0.22f),
            surfaceContainerLowest = cronchBlend(charred, primary, 0.05f),
            surfaceContainerLow = cronchBlend(charred, primary, 0.10f),
            surfaceContainer = cronchBlend(charred, primary, 0.14f),
            surfaceContainerHigh = cronchBlend(charred, primary, 0.19f),
            surfaceContainerHighest = cronchBlend(charred, primary, 0.25f),
            onBackground = CronchRanchCream,
            onSurface = CronchRanchCream,
            outline = cronchBlend(CronchMoaiStone, primary, 0.25f),
            outlineVariant = cronchBlend(CronchMoaiStone, primary, 0.15f),
        )
    } else {
        val bg = cronchBlend(cream, primary, 0.04f)
        copy(
            background = bg,
            surface = cronchBlend(cream, primary, 0.06f),
            surfaceVariant = cronchBlend(cream, primary, 0.12f),
            surfaceDim = cronchBlend(cream, primary, 0.10f),
            surfaceBright = cronchBlend(Color.White, primary, 0.05f),
            surfaceContainerLowest = cronchBlend(Color.White, primary, 0.03f),
            surfaceContainerLow = cronchBlend(cream, primary, 0.08f),
            surfaceContainer = cronchBlend(cream, primary, 0.11f),
            surfaceContainerHigh = cronchBlend(cream, primary, 0.15f),
            surfaceContainerHighest = cronchBlend(cream, primary, 0.20f),
            onBackground = CronchCoopBrown,
            onSurface = CronchCoopBrown,
            outline = cronchBlend(CronchMoaiStone, primary, 0.25f),
            outlineVariant = cronchBlend(CronchMoaiStone, primary, 0.15f),
        )
    }
}

@Composable
fun rememberKernelSUColorScheme(
    seedColor: Color,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
): ColorScheme {
    val context = LocalContext.current
    val seed = if (seedColor == Color.Unspecified) {
        (if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
    } else {
        seedColor
    }
    return rememberDynamicColorScheme(
        seedColor = seed,
        isDark = isDark,
        isAmoled = isAmoled,
        style = paletteStyle,
        specVersion = colorSpec.effectiveFor(paletteStyle),
    ).amoledBackground(isAmoled)
}

@Composable
fun ColorScheme.animateAsState(): ColorScheme {
    // Respect the system "remove animations" setting: when the animator
    // duration scale is 0, theme changes apply instantly with no animation.
    val context = LocalContext.current
    val animationsDisabled = Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
    if (animationsDisabled) return this

    @Composable
    fun animateColor(color: Color): Color = animateColorAsState(
        targetValue = color,
        animationSpec = spring(),
        label = "theme_color_animation"
    ).value

    return ColorScheme(
        primary = animateColor(primary),
        onPrimary = animateColor(onPrimary),
        primaryContainer = animateColor(primaryContainer),
        onPrimaryContainer = animateColor(onPrimaryContainer),
        inversePrimary = animateColor(inversePrimary),
        secondary = animateColor(secondary),
        onSecondary = animateColor(onSecondary),
        secondaryContainer = animateColor(secondaryContainer),
        onSecondaryContainer = animateColor(onSecondaryContainer),
        tertiary = animateColor(tertiary),
        onTertiary = animateColor(onTertiary),
        tertiaryContainer = animateColor(tertiaryContainer),
        onTertiaryContainer = animateColor(onTertiaryContainer),
        background = animateColor(background),
        onBackground = animateColor(onBackground),
        surface = animateColor(surface),
        onSurface = animateColor(onSurface),
        surfaceVariant = animateColor(surfaceVariant),
        onSurfaceVariant = animateColor(onSurfaceVariant),
        surfaceTint = animateColor(surfaceTint),
        inverseSurface = animateColor(inverseSurface),
        inverseOnSurface = animateColor(inverseOnSurface),
        error = animateColor(error),
        onError = animateColor(onError),
        errorContainer = animateColor(errorContainer),
        onErrorContainer = animateColor(onErrorContainer),
        outline = animateColor(outline),
        outlineVariant = animateColor(outlineVariant),
        scrim = animateColor(scrim),
        surfaceBright = animateColor(surfaceBright),
        surfaceDim = animateColor(surfaceDim),
        surfaceContainer = animateColor(surfaceContainer),
        surfaceContainerHigh = animateColor(surfaceContainerHigh),
        surfaceContainerHighest = animateColor(surfaceContainerHighest),
        surfaceContainerLow = animateColor(surfaceContainerLow),
        surfaceContainerLowest = animateColor(surfaceContainerLowest),

        primaryFixed = animateColor(primaryFixed),
        primaryFixedDim = animateColor(primaryFixedDim),
        onPrimaryFixed = animateColor(onPrimaryFixed),
        onPrimaryFixedVariant = animateColor(onPrimaryFixedVariant),
        secondaryFixed = animateColor(secondaryFixed),
        secondaryFixedDim = animateColor(secondaryFixedDim),
        onSecondaryFixed = animateColor(onSecondaryFixed),
        onSecondaryFixedVariant = animateColor(onSecondaryFixedVariant),
        tertiaryFixed = animateColor(tertiaryFixed),
        tertiaryFixedDim = animateColor(tertiaryFixedDim),
        onTertiaryFixed = animateColor(onTertiaryFixed),
        onTertiaryFixedVariant = animateColor(onTertiaryFixedVariant)
    )
}