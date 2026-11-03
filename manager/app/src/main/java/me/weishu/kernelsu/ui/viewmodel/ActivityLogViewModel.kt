package me.weishu.kernelsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.weishu.kernelsu.ui.util.CronchLog

/**
 * The Rainbow Roost: the app's own Activity Log, kept strictly separate
 * from the root operation log (sulog) and from logcat (Phase 3).
 *
 * Live/paused semantics: pausing freezes the DISPLAYED list at a snapshot;
 * capture keeps running in [CronchLog]'s ring buffer, nothing is lost, and
 * [pendingCount] counts entries appended after the pause point. Resume
 * re-syncs to the live buffer. The buffer itself is capped at
 * [CronchLog.MAX_LINES]; the cap is fixed in Phase 1 and shown in the UI.
 */
class ActivityLogViewModel : ViewModel() {

    private val _paused = MutableStateFlow(false)
    val paused: StateFlow<Boolean> = _paused.asStateFlow()

    private val _autoScroll = MutableStateFlow(true)
    val autoScroll: StateFlow<Boolean> = _autoScroll.asStateFlow()

    private val _searchText = MutableStateFlow("")
    val searchText: StateFlow<String> = _searchText.asStateFlow()

    private val _selectedSeverities =
        MutableStateFlow(CronchLog.Severity.entries.toSet())
    val selectedSeverities: StateFlow<Set<CronchLog.Severity>> =
        _selectedSeverities.asStateFlow()

    private val _selectedComponent = MutableStateFlow<String?>(null)
    val selectedComponent: StateFlow<String?> = _selectedComponent.asStateFlow()

    /** Frozen list while paused; empty while live. */
    private val snapshotFlow = MutableStateFlow<List<CronchLog.Entry>>(emptyList())

    /** Last sequence number in the pause snapshot; -1 while live. */
    private val pausedAtSeqFlow = MutableStateFlow(-1L)

    private val _visibleEntries = MutableStateFlow<List<CronchLog.Entry>>(emptyList())
    val visibleEntries: StateFlow<List<CronchLog.Entry>> = _visibleEntries.asStateFlow()

    private val _components = MutableStateFlow<List<String>>(emptyList())
    val components: StateFlow<List<String>> = _components.asStateFlow()

    private val _pendingCount = MutableStateFlow(0)
    val pendingCount: StateFlow<Int> = _pendingCount.asStateFlow()

    private val _totalCount = MutableStateFlow(0)
    val totalCount: StateFlow<Int> = _totalCount.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.Default) {
            // combine() has typed overloads only up to 5 flows in
            // kotlinx-coroutines 1.11.0; a 7-flow call falls back to the
            // vararg form and its lambda parameters cannot be inferred.
            // Nest the 3 filter inputs into one Triple so the outer call
            // has 5 flows. Semantics unchanged.
            combine(
                CronchLog.entries,
                _paused,
                snapshotFlow,
                pausedAtSeqFlow,
                combine(_searchText, _selectedSeverities, _selectedComponent) { searchText, severities, component ->
                    Triple(searchText, severities, component)
                },
            ) { live: List<CronchLog.Entry>, paused: Boolean, snapshot: List<CronchLog.Entry>, pausedAtSeq: Long, filter: Triple<String, Set<CronchLog.Severity>, String?> ->
                val (searchText, severities, component) = filter
                val query = searchText.trim().lowercase()

                _components.value = live.map { it.tag }.distinct().sorted()
                _totalCount.value = live.size
                _pendingCount.value =
                    if (paused) live.count { it.seq > pausedAtSeq } else 0

                val base = if (paused) snapshot else live
                base.filter { entry ->
                    (entry.severity in severities) &&
                        (component == null || entry.tag == component) &&
                        (query.isEmpty() || entryMatches(entry, query))
                }
            }.collect { visible ->
                _visibleEntries.value = visible
            }
        }
    }

    private fun entryMatches(entry: CronchLog.Entry, query: String): Boolean {
        return entry.message.lowercase().contains(query) ||
            entry.tag.lowercase().contains(query) ||
            entry.event.lowercase().contains(query) ||
            entry.severity.name.lowercase().contains(query) ||
            (entry.details?.lowercase()?.contains(query) == true)
    }

    fun setPaused(paused: Boolean) {
        if (paused == _paused.value) return
        if (paused) {
            // Log first, then snapshot: the pause event itself is inside the
            // frozen list, so the pending counter starts at 0.
            CronchLog.log(
                CronchLog.Severity.Info,
                "Coop",
                "activity_log_pause",
                "Rainbow Roost paused",
            )
            val live = CronchLog.entries.value
            snapshotFlow.value = live
            pausedAtSeqFlow.value = live.lastOrNull()?.seq ?: -1L
            _paused.value = true
        } else {
            snapshotFlow.value = emptyList()
            pausedAtSeqFlow.value = -1L
            _paused.value = false
            CronchLog.log(
                CronchLog.Severity.Info,
                "Coop",
                "activity_log_pause",
                "Rainbow Roost resumed",
            )
        }
    }

    fun setAutoScroll(enabled: Boolean) {
        _autoScroll.value = enabled
    }

    fun setSearchText(text: String) {
        _searchText.value = text
    }

    fun toggleSeverity(severity: CronchLog.Severity) {
        _selectedSeverities.update { current ->
            val next = current.toMutableSet().apply {
                if (!add(severity)) remove(severity)
            }
            if (next.isEmpty()) CronchLog.Severity.entries.toSet() else next
        }
    }

    fun selectAllSeverities() {
        _selectedSeverities.value = CronchLog.Severity.entries.toSet()
    }

    fun setComponent(component: String?) {
        _selectedComponent.value = component
    }

    fun clear() {
        CronchLog.clear()
        if (_paused.value) {
            snapshotFlow.value = CronchLog.entries.value
            pausedAtSeqFlow.value =
                CronchLog.entries.value.lastOrNull()?.seq ?: -1L
        }
    }
}
