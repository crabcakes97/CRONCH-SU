package me.weishu.kernelsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.weishu.kernelsu.data.repository.SettingsRepository
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ui.util.SulogEntry
import me.weishu.kernelsu.ui.util.SulogEventFilter
import me.weishu.kernelsu.ui.util.SulogFile
import me.weishu.kernelsu.ui.util.SulogStatus
import me.weishu.kernelsu.ui.util.SulogStatusCounts
import me.weishu.kernelsu.ui.util.defaultSulogStatuses
import me.weishu.kernelsu.ui.util.SulogFileCleanAction
import me.weishu.kernelsu.ui.util.buildVisibleSulogEntries
import me.weishu.kernelsu.ui.util.cleanSulogFile
import me.weishu.kernelsu.ui.util.countSulogStatuses
import me.weishu.kernelsu.ui.util.defaultSulogEventFilters
import me.weishu.kernelsu.ui.util.deleteSulogFile
import me.weishu.kernelsu.ui.util.listSulogFiles
import me.weishu.kernelsu.ui.util.parseSulogLines
import me.weishu.kernelsu.ui.util.readSulogFile
import me.weishu.kernelsu.ui.util.resolveSulogFileCleanAction

data class SulogUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val sulogStatus: String = "",
    val isSulogEnabled: Boolean = false,
    val searchText: String = "",
    val selectedFilters: Set<SulogEventFilter> = defaultSulogEventFilters(),
    val selectedStatuses: Set<SulogStatus> = defaultSulogStatuses(),
    val statusCounts: SulogStatusCounts = SulogStatusCounts(),
    val files: List<SulogFile> = emptyList(),
    val selectedFilePath: String? = null,
    val entries: List<SulogEntry> = emptyList(),
    val visibleEntries: List<SulogEntry> = emptyList(),
    val errorMessage: String? = null,
)

class SulogViewModel(
    private val repo: SettingsRepository = SettingsRepositoryImpl(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(SulogUiState())
    val uiState: StateFlow<SulogUiState> = _uiState.asStateFlow()
    private val entriesFlow = MutableStateFlow<List<SulogEntry>>(emptyList())
    private val searchTextFlow = MutableStateFlow("")
    private val selectedFiltersFlow = MutableStateFlow(defaultSulogEventFilters())
    private val selectedStatusesFlow = MutableStateFlow(defaultSulogStatuses())

    private var refreshJob: Job? = null

    init {
        val savedFilters = repo.suLogFilters
            ?.mapNotNull { raw -> SulogEventFilter.entries.firstOrNull { it.name == raw } }
            ?.toSet()
            ?.ifEmpty { defaultSulogEventFilters() }
            ?: defaultSulogEventFilters()
        selectedFiltersFlow.value = savedFilters
        _uiState.update { it.copy(selectedFilters = savedFilters) }

        viewModelScope.launch(Dispatchers.Default) {
            combine(entriesFlow, searchTextFlow, selectedFiltersFlow, selectedStatusesFlow) { entries, searchText, selectedFilters, selectedStatuses ->
                buildVisibleSulogEntries(entries, searchText, selectedFilters, selectedStatuses)
            }.collect { visibleEntries ->
                _uiState.update { it.copy(visibleEntries = visibleEntries) }
            }
        }
        viewModelScope.launch(Dispatchers.Default) {
            entriesFlow.collect { entries ->
                _uiState.update { it.copy(statusCounts = countSulogStatuses(entries)) }
            }
        }
    }

    fun refresh(preferredFilePath: String? = _uiState.value.selectedFilePath) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                val sulogStatus = repo.getSulogStatus()
                val isSulogEnabled = if (sulogStatus == "forced") {
                    repo.getSulogValue() == 1L
                } else {
                    repo.getSulogPersistValue() == 1L
                }
                val files = listSulogFiles()
                currentCoroutineContext().ensureActive()
                val selectedFile = when {
                    files.isEmpty() -> null
                    preferredFilePath != null -> files.firstOrNull { it.path == preferredFilePath } ?: files.first()
                    else -> files.first()
                }
                val logLines = selectedFile?.let { readSulogFile(it.path) }.orEmpty()
                val entries = parseSulogLines(logLines)
                val currentState = _uiState.value
                currentCoroutineContext().ensureActive()
                entriesFlow.value = entries
                SulogUiState(
                    isLoading = false,
                    sulogStatus = sulogStatus,
                    isSulogEnabled = isSulogEnabled,
                    searchText = currentState.searchText,
                    selectedFilters = currentState.selectedFilters,
                    selectedStatuses = currentState.selectedStatuses,
                    statusCounts = countSulogStatuses(entries),
                    files = files,
                    selectedFilePath = selectedFile?.path,
                    entries = entries,
                    visibleEntries = currentState.visibleEntries,
                )
            }.onSuccess { state ->
                _uiState.value = state
            }.onFailure { error ->
                if (error is kotlinx.coroutines.CancellationException) {
                    throw error
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    fun refreshLatest() {
        refresh(preferredFilePath = null)
    }

    fun enableSulog() {
        val preferredFilePath = _uiState.value.selectedFilePath
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            if (repo.setSulogEnabled(true)) {
                repo.execKsudFeatureSave()
            }
            refresh(preferredFilePath)
        }
    }

    fun cleanFile() {
        val currentState = _uiState.value
        val path = currentState.selectedFilePath ?: return
        val cleanAction = resolveSulogFileCleanAction(currentState.files, path)
        viewModelScope.launch(Dispatchers.IO) {
            when (cleanAction) {
                SulogFileCleanAction.Clear -> cleanSulogFile(path)
                SulogFileCleanAction.Delete -> deleteSulogFile(path)
            }
            refresh(path)
        }
    }

    fun setSearchText(searchText: String) {
        searchTextFlow.value = searchText
        _uiState.update { currentState ->
            currentState.copy(
                searchText = searchText,
            )
        }
    }

    fun toggleFilter(filter: SulogEventFilter) {
        _uiState.update { currentState ->
            val selectedFilters = currentState.selectedFilters.toMutableSet().apply {
                if (!add(filter)) remove(filter)
            }
            selectedFiltersFlow.value = selectedFilters
            repo.suLogFilters = selectedFilters.map { it.name }.toSet()
            currentState.copy(
                selectedFilters = selectedFilters,
            )
        }
    }

    /** Outcome filter. Session-only on purpose: the persisted suLogFilters
     *  pref stores event filters; outcomes reset to all on entering the
     *  screen. Deselecting every outcome re-selects all, so the list can
     *  never be emptied by accident. */
    fun toggleStatusFilter(status: SulogStatus) {
        _uiState.update { currentState ->
            val selectedStatuses = currentState.selectedStatuses.toMutableSet().apply {
                if (!add(status)) remove(status)
            }.ifEmpty { defaultSulogStatuses() }.toSet()
            selectedStatusesFlow.value = selectedStatuses
            currentState.copy(
                selectedStatuses = selectedStatuses,
            )
        }
    }
}
