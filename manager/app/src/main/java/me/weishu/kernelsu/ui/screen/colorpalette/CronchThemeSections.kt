package me.weishu.kernelsu.ui.screen.colorpalette

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.materialkolor.PaletteStyle
import com.materialkolor.dynamiccolor.ColorSpec
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.CRONCH_THEMES
import me.weishu.kernelsu.ui.theme.CRONCH_THEME_CUSTOM_ID
import me.weishu.kernelsu.ui.theme.CronchThemePreset
import me.weishu.kernelsu.ui.theme.rememberKernelSUColorScheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme as miuixColorScheme

// ---------------------------------------------------------------------------
// Shared helpers: hex <-> ARGB. Alpha is always forced opaque so a custom
// accent can never produce a transparent/invisible control.
// ---------------------------------------------------------------------------

private val HEX6 = Regex("^#?[0-9a-fA-F]{6}$")

internal fun cronchArgbToHex(argb: Int): String =
    "%06X".format(argb and 0x00FFFFFF)

internal fun cronchHexToArgbOrNull(hex: String): Int? {
    val clean = hex.trim().removePrefix("#")
    if (!HEX6.matches(hex.trim())) return null
    return try {
        (clean.toInt(16) or 0xFF000000.toInt())
    } catch (_: NumberFormatException) {
        null
    }
}

/** Text color that stays readable on top of an arbitrary seed color: black on
 *  light seeds, white on dark ones (relative luminance, sRGB). */
internal fun cronchOnSeedColor(argb: Int): Color {
    fun channel(c: Int): Double {
        val s = c / 255.0
        return if (s <= 0.03928) s / 12.92 else Math.pow((s + 0.055) / 1.055, 2.4)
    }
    val r = channel((argb shr 16) and 0xFF)
    val g = channel((argb shr 8) and 0xFF)
    val b = channel(argb and 0xFF)
    val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
    return if (luminance > 0.45) Color.Black else Color.White
}
@Composable
private fun CronchPresetChip(
    preset: CronchThemePreset,
    selected: Boolean,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Color(preset.seedArgb) else Color(preset.seedArgb).copy(alpha = 0.4f),
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick),
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(preset.seedArgb)),
            )
            Text(
                text = stringResource(preset.nameRes),
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

/** Live preview of a candidate seed, rendered with the REAL scheme the app
 *  will use (same generator as the active theme). What you see is what the
 *  whole app will look like — contrast is guaranteed by the generator. */
@Composable
private fun CronchColorPreview(
    seedArgb: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    modifier: Modifier = Modifier,
) {
    val scheme = rememberKernelSUColorScheme(
        seedColor = Color(seedArgb),
        isDark = isDark,
        isAmoled = isAmoled,
        paletteStyle = paletteStyle,
        colorSpec = colorSpec,
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = scheme.surface,
        contentColor = scheme.onSurface,
        shape = RoundedCornerShape(12.dp),
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(R.string.cronch_custom_color_preview),
                style = MaterialTheme.typography.labelMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    color = scheme.primary,
                    contentColor = scheme.onPrimary,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "Peck",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
                Surface(
                    color = scheme.secondaryContainer,
                    contentColor = scheme.onSecondaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = "Coop",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            Text(
                text = "CRONCH. PECKS COOL RANCH DORITO \uD83D\uDC14\uD83C\uDF2E",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Material (default UI mode) section
// ---------------------------------------------------------------------------

@Composable
fun CronchThemeSectionMaterial(
    currentThemeId: String,
    currentKeyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    onApplyPreset: (CronchThemePreset) -> Unit,
    onSetKeyColor: (Int) -> Unit,
    onSetCronchThemeId: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.cronch_themes_title),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = stringResource(R.string.cronch_themes_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            CRONCH_THEMES.forEach { preset ->
                CronchPresetChip(
                    preset = preset,
                    selected = currentThemeId == preset.id,
                    containerColor = if (currentThemeId == preset.id) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    },
                    contentColor = if (currentThemeId == preset.id) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    },
                    onClick = { onApplyPreset(preset) },
                )
            }
        }
        CronchCustomColorPickerMaterial(
            currentKeyColor = currentKeyColor,
            isDark = isDark,
            isAmoled = isAmoled,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
            onSetKeyColor = onSetKeyColor,
            onSetCronchThemeId = onSetCronchThemeId,
        )
    }
}

@Composable
private fun CronchCustomColorPickerMaterial(
    currentKeyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    onSetKeyColor: (Int) -> Unit,
    onSetCronchThemeId: (String) -> Unit,
) {
    val startColor = if (currentKeyColor != 0) currentKeyColor else 0xFFE8590C.toInt()
    // Single source of truth: one ARGB color. Sliders write channels into
    // it; the hex field edits it when the text parses. The hex text follows
    // the color via LaunchedEffect so it can never lag the sliders by a tick.
    var color by remember(currentKeyColor) { mutableIntStateOf(startColor) }
    var hexText by remember(currentKeyColor) { mutableStateOf(cronchArgbToHex(startColor)) }
    LaunchedEffect(color) {
        hexText = cronchArgbToHex(color)
    }
    val hexValid = cronchHexToArgbOrNull(hexText) != null
    val candidate = color

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.cronch_custom_color_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.cronch_custom_color_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        listOf(
            Triple("R", (color shr 16) and 0xFF) { v: Int -> color = (color and 0xFF00FFFF.toInt()) or (v shl 16) },
            Triple("G", (color shr 8) and 0xFF) { v: Int -> color = (color and 0xFFFF00FF.toInt()) or (v shl 8) },
            Triple("B", color and 0xFF) { v: Int -> color = (color and 0xFFFFFF00.toInt()) or v },
        ).forEach { (label, value, setValue) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(text = label, modifier = Modifier.padding(end = 4.dp))
                Slider(
                    value = value.toFloat(),
                    onValueChange = { setValue(it.toInt()) },
                    valueRange = 0f..255f,
                    steps = 254,
                    modifier = Modifier.weight(1f),
                )
                Text(text = value.toString(), modifier = Modifier.padding(start = 4.dp))
            }
        }
        OutlinedTextField(
            value = hexText,
            onValueChange = { input ->
                hexText = input
                cronchHexToArgbOrNull(input)?.let { color = it }
            },
            label = { Text(stringResource(R.string.cronch_custom_color_hex_hint)) },
            isError = !hexValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        CronchColorPreview(
            seedArgb = candidate,
            isDark = isDark,
            isAmoled = isAmoled,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onSetKeyColor(candidate)
                    onSetCronchThemeId(CRONCH_THEME_CUSTOM_ID)
                },
                enabled = hexValid && candidate != currentKeyColor,
            ) {
                Text(stringResource(R.string.cronch_custom_color_apply))
            }
            TextButton(
                onClick = {
                    onSetKeyColor(0)
                    onSetCronchThemeId("")
                },
                enabled = currentKeyColor != 0,
            ) {
                Text(stringResource(R.string.cronch_custom_color_reset))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Miuix UI mode section
// ---------------------------------------------------------------------------

@Composable
fun CronchThemeSectionMiuix(
    currentThemeId: String,
    currentKeyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    onApplyPreset: (CronchThemePreset) -> Unit,
    onSetKeyColor: (Int) -> Unit,
    onSetCronchThemeId: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            MiuixText(stringResource(R.string.cronch_themes_title))
            MiuixText(stringResource(R.string.cronch_themes_subtitle))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CRONCH_THEMES.forEach { preset ->
                    CronchPresetChip(
                        preset = preset,
                        selected = currentThemeId == preset.id,
                        containerColor = Color(preset.seedArgb).copy(
                            alpha = if (currentThemeId == preset.id) 0.32f else 0.14f,
                        ),
                        contentColor = miuixColorScheme.onBackground,
                        onClick = { onApplyPreset(preset) },
                    )
                }
            }
            CronchCustomColorPickerMiuix(
                currentKeyColor = currentKeyColor,
                isDark = isDark,
                isAmoled = isAmoled,
                paletteStyle = paletteStyle,
                colorSpec = colorSpec,
                onSetKeyColor = onSetKeyColor,
                onSetCronchThemeId = onSetCronchThemeId,
            )
        }
    }
}

@Composable
private fun CronchCustomColorPickerMiuix(
    currentKeyColor: Int,
    isDark: Boolean,
    isAmoled: Boolean,
    paletteStyle: PaletteStyle,
    colorSpec: ColorSpec.SpecVersion,
    onSetKeyColor: (Int) -> Unit,
    onSetCronchThemeId: (String) -> Unit,
) {
    val startColor = if (currentKeyColor != 0) currentKeyColor else 0xFFE8590C.toInt()
    // Single source of truth, same as the Material picker: one ARGB color;
    // hex text follows it via LaunchedEffect and edits it when it parses.
    var color by remember(currentKeyColor) { mutableIntStateOf(startColor) }
    var hexText by remember(currentKeyColor) { mutableStateOf(cronchArgbToHex(startColor)) }
    LaunchedEffect(color) {
        hexText = cronchArgbToHex(color)
    }
    val hexValid = cronchHexToArgbOrNull(hexText) != null
    val candidate = color

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        MiuixText(stringResource(R.string.cronch_custom_color_title))
        MiuixText(stringResource(R.string.cronch_custom_color_subtitle))
        listOf(
            Triple("R", (color shr 16) and 0xFF) { v: Int -> color = (color and 0xFF00FFFF.toInt()) or (v shl 16) },
            Triple("G", (color shr 8) and 0xFF) { v: Int -> color = (color and 0xFFFF00FF.toInt()) or (v shl 8) },
            Triple("B", color and 0xFF) { v: Int -> color = (color and 0xFFFFFF00.toInt()) or v },
        ).forEach { (label, value, setValue) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MiuixText(label)
                MiuixSlider(
                    value = value.toFloat(),
                    onValueChange = { setValue(it.toInt()) },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f),
                )
                MiuixText(value.toString())
            }
        }
        TextField(
            value = hexText,
            onValueChange = { input ->
                hexText = input
                cronchHexToArgbOrNull(input)?.let { color = it }
            },
            label = stringResource(R.string.cronch_custom_color_hex_hint),
        )
        CronchColorPreview(
            seedArgb = candidate,
            isDark = isDark,
            isAmoled = isAmoled,
            paletteStyle = paletteStyle,
            colorSpec = colorSpec,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val canApply = hexValid && candidate != currentKeyColor
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(enabled = canApply) {
                        onSetKeyColor(candidate)
                        onSetCronchThemeId(CRONCH_THEME_CUSTOM_ID)
                    },
                color = Color(candidate).copy(alpha = if (canApply) 1f else 0.4f),
                contentColor = cronchOnSeedColor(candidate),
                shape = RoundedCornerShape(20.dp),
            ) {
                Text(
                    text = stringResource(R.string.cronch_custom_color_apply),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            TextButton(
                onClick = {
                    onSetKeyColor(0)
                    onSetCronchThemeId("")
                },
            ) {
                Text(text = stringResource(R.string.cronch_custom_color_reset))
            }
        }
    }
}
