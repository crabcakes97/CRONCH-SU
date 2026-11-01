package me.weishu.kernelsu.ui.screen.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import me.weishu.kernelsu.ui.util.CronchLog
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

/**
 * The Coop log: a verbose, scrolling console pinned at the top of the home
 * screen showing everything Cronch SU is doing, as it happens. Auto-follows
 * new lines while the user is at the bottom; if the user scrolls up,
 * auto-follow pauses and a "Latest" button appears to jump back down.
 * Rainbow lettering is the default (persisted toggle in the header).
 *
 * Two thin wrappers supply theme-appropriate colors because the app has two
 * UI modes; the core below takes explicit colors so it renders correctly
 * under either theme.
 */
@Composable
fun CronchLogConsoleMaterial(modifier: Modifier = Modifier) {
    CronchLogConsole(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        mutedColor = MaterialTheme.colorScheme.onSurfaceVariant,
        accentColor = MaterialTheme.colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
fun CronchLogConsoleMiuix(modifier: Modifier = Modifier) {
    CronchLogConsole(
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onBackground,
        mutedColor = colorScheme.onSurfaceVariantSummary,
        accentColor = colorScheme.primary,
        modifier = modifier,
    )
}

@Composable
private fun CronchLogConsole(
    containerColor: Color,
    contentColor: Color,
    mutedColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val entries by CronchLog.entries.collectAsStateWithLifecycle()
    val rainbow by CronchLog.rainbowEnabled.collectAsStateWithLifecycle()
    val isDark = isInDarkTheme()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        CronchLog.ensurePrefsLoaded()
    }

    // At the bottom (or empty) => follow new lines. Read from the lazy
    // layout info so a user scroll-up pauses auto-follow on its own.
    val atBottom by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index
            lastVisible == null || lastVisible >= info.totalItemsCount - 1
        }
    }

    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty() && atBottom) {
            listState.scrollToItem(entries.lastIndex)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = stringResource(R.string.cronch_log_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = contentColor,
                    modifier = Modifier.weight(1f),
                )
                if (!atBottom && entries.isNotEmpty()) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                runCatching { listState.animateScrollToItem(entries.lastIndex) }
                            }
                        },
                    ) {
                        Text(
                            text = stringResource(R.string.cronch_log_latest),
                            color = accentColor,
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.cronch_log_rainbow),
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                )
                Switch(
                    checked = rainbow,
                    onCheckedChange = { enabled ->
                        CronchLog.setRainbowEnabled(enabled)
                        CronchLog.log("Coop", "Rainbow log ${if (enabled) "on" else "off"}")
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = accentColor,
                        checkedTrackColor = accentColor.copy(alpha = 0.5f),
                        uncheckedThumbColor = mutedColor,
                        uncheckedTrackColor = mutedColor.copy(alpha = 0.3f),
                        uncheckedBorderColor = mutedColor,
                    ),
                )
                TextButton(
                    onClick = { CronchLog.clear() },
                ) {
                    Text(
                        text = stringResource(R.string.cronch_log_clear),
                        color = contentColor,
                    )
                }
                TextButton(
                    onClick = { copyLogToClipboard(context) },
                ) {
                    Text(
                        text = stringResource(R.string.cronch_log_copy),
                        color = contentColor,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
            ) {
                if (entries.isEmpty()) {
                    Text(
                        text = stringResource(R.string.cronch_log_empty),
                        style = MaterialTheme.typography.bodySmall,
                        color = mutedColor,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    LazyColumn(state = listState) {
                        items(entries, key = { it.seq }) { entry ->
                            Text(
                                text = "[${CronchLog.formatTime(entry.timeMillis)}] [${entry.tag}] ${entry.message}",
                                color = if (rainbow) CronchLog.rainbowColor(entry, isDark) else contentColor,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.cronch_log_footer, entries.size, CronchLog.MAX_LINES),
                style = MaterialTheme.typography.bodySmall,
                color = mutedColor,
            )
        }
    }
}

private fun copyLogToClipboard(context: Context) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Cronch SU log", CronchLog.formatForCopy())
        )
        Toast.makeText(context, R.string.cronch_log_copied, Toast.LENGTH_SHORT).show()
    }.onFailure { e ->
        CronchLog.log("Coop", "Copy log failed: ${e.message ?: "unavailable"}")
    }
}
