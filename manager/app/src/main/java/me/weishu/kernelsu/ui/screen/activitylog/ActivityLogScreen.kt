package me.weishu.kernelsu.ui.screen.activitylog

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.screen.superuser.rememberExportTextLauncher
import me.weishu.kernelsu.ui.util.CronchLog
import me.weishu.kernelsu.ui.viewmodel.ActivityLogViewModel

/**
 * The Rainbow Roost: Cronch SU's own Activity Log as a dedicated screen.
 * Strictly separate from the root operation log (sulog files, the Ledger
 * of Pecks) and from logcat (Phase 3). Every entry is a real event with a
 * real timestamp; nothing here is simulated.
 *
 * NEW LORE: the "Rainbow Roost" is the high perch where the Coop watches
 * everything the app itself does. See LORE-UI-MAP.md.
 */
@Composable
fun ActivityLogScreen() {
    val navigator = LocalNavigator.current
    val uiMode = LocalUiMode.current
    val viewModel = viewModel<ActivityLogViewModel>()
    val context = LocalContext.current

    val entries by viewModel.visibleEntries.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val pendingCount by viewModel.pendingCount.collectAsStateWithLifecycle()
    val paused by viewModel.paused.collectAsStateWithLifecycle()
    val autoScroll by viewModel.autoScroll.collectAsStateWithLifecycle()
    val searchText by viewModel.searchText.collectAsStateWithLifecycle()
    val selectedSeverities by viewModel.selectedSeverities.collectAsStateWithLifecycle()
    val components by viewModel.components.collectAsStateWithLifecycle()
    val selectedComponent by viewModel.selectedComponent.collectAsStateWithLifecycle()
    val rainbow by CronchLog.rainbowEnabled.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        CronchLog.ensurePrefsLoaded()
        CronchLog.log(
            CronchLog.Severity.Info,
            "Coop",
            "activity_log_opened",
            "Rainbow Roost opened",
        )
    }

    val exportLog = rememberExportTextLauncher(
        fileName = "cronch-su-activity-log.txt",
        buildText = CronchLog::formatForExport,
        onDone = { success ->
            if (success) {
                CronchLog.log(
                    CronchLog.Severity.Info,
                    "Coop",
                    "activity_log_export",
                    "Rainbow Roost exported",
                )
                Toast.makeText(context, R.string.activity_log_exported, Toast.LENGTH_SHORT).show()
            } else {
                CronchLog.log(
                    CronchLog.Severity.Warning,
                    "Coop",
                    "activity_log_export",
                    "Rainbow Roost export failed",
                )
                Toast.makeText(
                    context,
                    context.getString(R.string.activity_log_export_failed, "could not write file"),
                    Toast.LENGTH_SHORT,
                ).show()
            }
        },
    )

    val state = ActivityLogScreenState(
        entries = entries,
        totalCount = totalCount,
        pendingCount = pendingCount,
        paused = paused,
        autoScroll = autoScroll,
        searchText = searchText,
        selectedSeverities = selectedSeverities,
        components = components,
        selectedComponent = selectedComponent,
        rainbow = rainbow,
    )
    val actions = ActivityLogActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onPauseToggle = { viewModel.setPaused(!paused) },
        onAutoScrollToggle = { viewModel.setAutoScroll(!autoScroll) },
        onSearchTextChange = viewModel::setSearchText,
        onToggleSeverity = viewModel::toggleSeverity,
        onSelectAllSeverities = viewModel::selectAllSeverities,
        onSelectComponent = viewModel::setComponent,
        onClear = viewModel::clear,
        onCopy = { copyLogToClipboard(context) },
        onExport = exportLog,
        onRainbowToggle = { enabled ->
            CronchLog.setRainbowEnabled(enabled)
            CronchLog.log(
                CronchLog.Severity.Info,
                "Coop",
                "activity_log_rainbow",
                "Rainbow log ${if (enabled) "on" else "off"}",
            )
        },
    )

    when (uiMode) {
        UiMode.Material -> ActivityLogScreenMaterial(state, actions)
        UiMode.Miuix -> ActivityLogScreenMiuix(state, actions)
    }
}

private fun copyLogToClipboard(context: Context) {
    runCatching {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText("Cronch SU activity log", CronchLog.formatForCopy())
        )
        Toast.makeText(context, R.string.cronch_log_copied, Toast.LENGTH_SHORT).show()
    }.onFailure { e ->
        CronchLog.log(
            CronchLog.Severity.Warning,
            "Coop",
            "activity_log_copy",
            "Copy activity log failed: ${e.message ?: "unavailable"}",
        )
    }
}
