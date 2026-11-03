package me.weishu.kernelsu.ui.screen.activitylog

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.CronchCoopBrown
import me.weishu.kernelsu.ui.theme.CronchDisplayFontFamily
import me.weishu.kernelsu.ui.theme.CronchLogShape
import me.weishu.kernelsu.ui.util.CronchLog

/**
 * Shared Rainbow Roost content, rendered identically under both UI modes.
 * All colors are explicit (the log lives on fixed terminal surfaces, so
 * contrast holds in every one of the twenty flavor themes), and both
 * wrappers pass the same semantic colors in. The only animation is the
 * rainbow border: it moves only while the rainbow toggle is on, only
 * while this screen is composed, and never when the system animator
 * duration scale is 0 (reduced motion).
 *
 * Contrast note (fixed surfaces, theme-independent): the log surfaces are
 * [CronchLogDarkSurface] / [CronchLogLightSurface]; severity badge colors
 * below were picked against those two surfaces only, so no theme can wash
 * them out. Rainbow line colors come from [CronchLog.rainbowColor], which
 * already adapts to dark/light.
 */
@Composable
fun ActivityLogContent(
    state: ActivityLogScreenState,
    actions: ActivityLogActions,
    containerColor: Color,
    contentColor: Color,
    mutedColor: Color,
    accentColor: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var showComponentMenu by remember { mutableStateOf(false) }

    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index
            lastVisible == null || lastVisible >= info.totalItemsCount - 1
        }
    }

    LaunchedEffect(state.entries.size, state.paused) {
        if (!state.paused && state.autoScroll && state.entries.isNotEmpty() && atBottom) {
            listState.scrollToItem(state.entries.lastIndex)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .rainbowRoostBorder(enabled = state.rainbow, shape = CronchLogShape),
        shape = CronchLogShape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
            Text(
                text = stringResource(R.string.activity_log_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = CronchDisplayFontFamily
                ),
                color = contentColor,
                maxLines = 1,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.activity_log_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = mutedColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
            )

            // Status row: live/paused, entry counter, pending-while-paused.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = if (state.paused) {
                        stringResource(R.string.activity_log_paused)
                    } else {
                        stringResource(R.string.activity_log_live)
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (state.paused) mutedColor else accentColor,
                )
                Text(
                    text = stringResource(
                        R.string.cronch_log_footer,
                        state.totalCount,
                        CronchLog.MAX_LINES,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                )
                Spacer(modifier = Modifier.weight(1f))
                if (state.paused && state.pendingCount > 0) {
                    TextButton(
                        onClick = {
                            actions.onPauseToggle()
                            scope.launch {
                                runCatching { listState.scrollToItem(state.entries.lastIndex) }
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(
                                R.string.activity_log_new_entries,
                                state.pendingCount,
                            ),
                            color = accentColor,
                        )
                    }
                }
            }

            // Controls row: rainbow + follow switches, pause, burn, steal.
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(
                    text = stringResource(R.string.cronch_log_rainbow),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                )
                Switch(
                    checked = state.rainbow,
                    onCheckedChange = actions.onRainbowToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = mutedColor,
                        uncheckedTrackColor = mutedColor.copy(alpha = 0.3f),
                        uncheckedBorderColor = mutedColor,
                    ),
                )
                Text(
                    text = stringResource(R.string.activity_log_autoscroll),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                    modifier = Modifier.padding(start = 8.dp),
                )
                Switch(
                    checked = state.autoScroll,
                    onCheckedChange = { actions.onAutoScrollToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = mutedColor,
                        uncheckedTrackColor = mutedColor.copy(alpha = 0.3f),
                        uncheckedBorderColor = mutedColor,
                    ),
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = actions.onPauseToggle) {
                    Text(
                        text = stringResource(
                            if (state.paused) R.string.activity_log_resume else R.string.activity_log_pause
                        ),
                        color = contentColor,
                    )
                }
                TextButton(onClick = actions.onClear) {
                    Text(text = stringResource(R.string.cronch_log_clear), color = contentColor)
                }
                TextButton(onClick = actions.onCopy) {
                    Text(text = stringResource(R.string.cronch_log_copy), color = contentColor)
                }
                TextButton(onClick = actions.onExport) {
                    Text(text = stringResource(R.string.activity_log_export), color = contentColor)
                }
            }

            // Search.
            TextField(
                value = state.searchText,
                onValueChange = actions.onSearchTextChange,
                singleLine = true,
                placeholder = {
                    Text(
                        text = stringResource(R.string.activity_log_search_hint),
                        color = mutedColor,
                    )
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = mutedColor)
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = containerColor,
                    unfocusedContainerColor = containerColor,
                    disabledContainerColor = containerColor,
                    focusedTextColor = contentColor,
                    unfocusedTextColor = contentColor,
                    cursorColor = accentColor,
                    focusedIndicatorColor = accentColor,
                    unfocusedIndicatorColor = mutedColor.copy(alpha = 0.4f),
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )

            // Severity filter chips.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(
                    text = stringResource(R.string.activity_log_filter_severity),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                )
                SeverityFilterChip(
                    selected = state.selectedSeverities.size == CronchLog.Severity.entries.size,
                    label = stringResource(R.string.activity_log_filter_all),
                    contentColor = contentColor,
                    mutedColor = mutedColor,
                    accentColor = accentColor,
                    onClick = actions.onSelectAllSeverities,
                )
                CronchLog.Severity.entries.forEach { severity ->
                    SeverityFilterChip(
                        selected = severity in state.selectedSeverities,
                        label = severityLabel(severity),
                        contentColor = contentColor,
                        mutedColor = mutedColor,
                        accentColor = accentColor,
                        onClick = { actions.onToggleSeverity(severity) },
                    )
                }
            }

            // Component filter.
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.activity_log_filter_component),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                )
                Box {
                    TextButton(onClick = { showComponentMenu = true }) {
                        Text(
                            text = state.selectedComponent
                                ?: stringResource(R.string.activity_log_filter_all),
                            color = accentColor,
                        )
                    }
                    DropdownMenu(
                        expanded = showComponentMenu,
                        onDismissRequest = { showComponentMenu = false },
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.activity_log_filter_all)) },
                            onClick = {
                                actions.onSelectComponent(null)
                                showComponentMenu = false
                            },
                        )
                        state.components.forEach { component ->
                            DropdownMenuItem(
                                text = { Text(component) },
                                onClick = {
                                    actions.onSelectComponent(component)
                                    showComponentMenu = false
                                },
                            )
                        }
                    }
                }
            }

            // Latest: its own right-aligned row, only when scrolled up.
            if (!atBottom && state.entries.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                runCatching { listState.animateScrollToItem(state.entries.lastIndex) }
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.cronch_log_latest),
                            color = accentColor,
                        )
                    }
                }
            }

            // Log lines.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
                    .padding(top = 6.dp),
            ) {
                when {
                    state.totalCount == 0 -> {
                        Text(
                            text = stringResource(R.string.activity_log_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedColor,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    state.entries.isEmpty() -> {
                        Text(
                            text = stringResource(R.string.activity_log_no_results),
                            style = MaterialTheme.typography.bodySmall,
                            color = mutedColor,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }

                    else -> {
                        LazyColumn(state = listState) {
                            items(state.entries, key = { it.seq }) { entry ->
                                val lineColor = if (state.rainbow) {
                                    CronchLog.rainbowColor(entry, isDark)
                                } else {
                                    contentColor
                                }
                                val badgeColor = severityBadgeColor(entry.severity, isDark)
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp),
                                ) {
                                    Text(
                                        text = CronchLog.formatTime(entry.timeMillis),
                                        color = lineColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 1,
                                        modifier = Modifier.width(58.dp),
                                    )
                                    Text(
                                        text = severityCode(entry.severity),
                                        color = badgeColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 1,
                                        modifier = Modifier.width(30.dp),
                                    )
                                    Text(
                                        text = entry.tag,
                                        color = lineColor,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        lineHeight = 15.sp,
                                        maxLines = 1,
                                        modifier = Modifier.width(64.dp),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = entry.message,
                                            color = lineColor,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            lineHeight = 15.sp,
                                        )
                                        entry.details?.let { details ->
                                            Text(
                                                text = details,
                                                color = mutedColor,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp,
                                                maxLines = 2,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SeverityFilterChip(
    selected: Boolean,
    label: String,
    contentColor: Color,
    mutedColor: Color,
    accentColor: Color,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = if (selected) {
            {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = CronchCoopBrown,
                )
            }
        } else {
            null
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = accentColor,
            selectedLabelColor = CronchCoopBrown,
            containerColor = Color.Transparent,
            labelColor = contentColor,
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) accentColor else mutedColor.copy(alpha = 0.5f),
        ),
    )
}

@Composable
private fun severityLabel(severity: CronchLog.Severity): String = when (severity) {
    CronchLog.Severity.Debug -> stringResource(R.string.activity_log_severity_debug)
    CronchLog.Severity.Info -> stringResource(R.string.activity_log_severity_info)
    CronchLog.Severity.Warning -> stringResource(R.string.activity_log_severity_warning)
    CronchLog.Severity.Error -> stringResource(R.string.activity_log_severity_error)
}

private fun severityCode(severity: CronchLog.Severity): String = when (severity) {
    CronchLog.Severity.Debug -> "DBG"
    CronchLog.Severity.Info -> "INF"
    CronchLog.Severity.Warning -> "WRN"
    CronchLog.Severity.Error -> "ERR"
}

/**
 * Fixed severity text colors, picked against the two fixed log surfaces
 * only: dark #241407, light #FFF3DC. Warning on light is a deep gold
 * (#8A5A00, ~5.3:1) rather than the bright gold used on dark, so the badge
 * stays readable in every theme.
 */
private fun severityBadgeColor(severity: CronchLog.Severity, isDark: Boolean): Color =
    when (severity) {
        CronchLog.Severity.Debug -> if (isDark) Color(0xFFB0B0B0) else Color(0xFF5A5A5A)
        CronchLog.Severity.Info -> if (isDark) Color(0xFFFFF1D6) else Color(0xFF3B2314)
        CronchLog.Severity.Warning -> if (isDark) Color(0xFFFFC107) else Color(0xFF8A5A00)
        CronchLog.Severity.Error -> if (isDark) Color(0xFFFF8A80) else Color(0xFFB3261E)
    }

// Fixed log surfaces live in the theme (CronchLogDarkSurface /
// CronchLogLightSurface); the wrappers pass them in, so this file needs
// no theme object and the contrast story stays theme-independent.

private val RAINBOW_BORDER_COLORS = listOf(
    Color(0xFFFF5252),
    Color(0xFFFF9800),
    Color(0xFFFFEB3B),
    Color(0xFF4CAF50),
    Color(0xFF2196F3),
    Color(0xFF3F51B5),
    Color(0xFF9C27B0),
)

/**
 * Rainbow border: static gradient when [enabled] is false or the system
 * animator scale is 0 (reduced motion); a slowly sliding gradient while
 * the screen is composed otherwise. Decorative only — never carries
 * meaning, so it needs no contrast guarantee.
 */
@Composable
private fun Modifier.rainbowRoostBorder(enabled: Boolean, shape: Shape): Modifier {
    if (!enabled) return this
    val context = LocalContext.current
    val animationsDisabled = remember {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    if (animationsDisabled) {
        return this.then(
            Modifier.border(
                width = 2.dp,
                brush = Brush.horizontalGradient(RAINBOW_BORDER_COLORS),
                shape = shape,
            )
        )
    }
    val transition = rememberInfiniteTransition(label = "rainbow_roost_border")
    val slide by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12000, easing = LinearEasing)
        ),
        label = "rainbow_roost_slide",
    )
    val span = 900f
    val brush = Brush.horizontalGradient(
        colors = RAINBOW_BORDER_COLORS,
        startX = -slide * span,
        endX = span - slide * span,
        tileMode = androidx.compose.ui.graphics.TileMode.Repeated,
    )
    return this.then(Modifier.border(width = 2.dp, brush = brush, shape = shape))
}
