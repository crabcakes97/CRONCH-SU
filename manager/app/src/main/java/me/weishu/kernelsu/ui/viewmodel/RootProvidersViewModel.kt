package me.weishu.kernelsu.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.ui.provider.ProviderDetection
import me.weishu.kernelsu.ui.provider.ProviderDetector
import me.weishu.kernelsu.ui.provider.ProviderInfo
import me.weishu.kernelsu.ui.provider.providerCatalog
import me.weishu.kernelsu.ui.util.CronchLog

data class RootProvidersUiState(
    val isLoading: Boolean = true,
    val providers: List<ProviderInfo> = emptyList(),
    val detection: ProviderDetection? = null,
)

class RootProvidersViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RootProvidersUiState())
    val uiState: StateFlow<RootProvidersUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val detection = withContext(Dispatchers.IO) { ProviderDetector.detect() }
            CronchLog.log(
                "Providers",
                "perch check: ksu-family=${detection.kernelSuFamilyDetected}, " +
                    "next=${detection.isNextFlavor}, " +
                    "magisk-binary=${detection.magiskBinaryPresent}",
            )
            _uiState.update {
                it.copy(
                    isLoading = false,
                    providers = providerCatalog(detection),
                    detection = detection,
                )
            }
        }
    }
}
