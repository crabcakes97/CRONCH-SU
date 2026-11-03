package me.weishu.kernelsu.ui.screen.activitylog

import androidx.compose.runtime.Immutable
import me.weishu.kernelsu.ui.util.CronchLog

@Immutable
data class ActivityLogScreenState(
    val entries: List<CronchLog.Entry> = emptyList(),
    val totalCount: Int = 0,
    val pendingCount: Int = 0,
    val paused: Boolean = false,
    val autoScroll: Boolean = true,
    val searchText: String = "",
    val selectedSeverities: Set<CronchLog.Severity> = CronchLog.Severity.entries.toSet(),
    val components: List<String> = emptyList(),
    val selectedComponent: String? = null,
    val rainbow: Boolean = true,
)

@Immutable
data class ActivityLogActions(
    val onBack: () -> Unit,
    val onPauseToggle: () -> Unit,
    val onAutoScrollToggle: () -> Unit,
    val onSearchTextChange: (String) -> Unit,
    val onToggleSeverity: (CronchLog.Severity) -> Unit,
    val onSelectAllSeverities: () -> Unit,
    val onSelectComponent: (String?) -> Unit,
    val onClear: () -> Unit,
    val onCopy: () -> Unit,
    val onExport: () -> Unit,
    val onRainbowToggle: (Boolean) -> Unit,
)
