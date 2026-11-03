package me.weishu.kernelsu.ui.screen.providers

import me.weishu.kernelsu.ui.provider.ProviderInfo

data class ProvidersScreenState(
    val isLoading: Boolean = true,
    val providers: List<ProviderInfo> = emptyList(),
)

data class ProvidersActions(
    val onBack: () -> Unit,
    val onRefresh: () -> Unit,
)
