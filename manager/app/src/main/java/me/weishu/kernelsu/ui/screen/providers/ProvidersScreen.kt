package me.weishu.kernelsu.ui.screen.providers

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.provider.CapabilityId
import me.weishu.kernelsu.ui.provider.ProviderId
import me.weishu.kernelsu.ui.viewmodel.RootProvidersViewModel

/**
 * The Five Perches — one card per root provider family. Detection states
 * come from live probes only (see ProviderDetector); capabilities come
 * from the upstream study notes. A provider that is present but not
 * driven by Cronch SU says so in plain words on its own card.
 *
 * NEW LORE: "The Five Perches" — each root family roosts on its own
 * perch in the Coop. See LORE-UI-MAP.md.
 */
@Composable
fun ProvidersScreen() {
    val navigator = LocalNavigator.current
    val uiMode = LocalUiMode.current
    val viewModel = viewModel<RootProvidersViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val state = ProvidersScreenState(
        isLoading = uiState.isLoading,
        providers = uiState.providers,
    )
    val actions = ProvidersActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onRefresh = viewModel::refresh,
    )

    when (uiMode) {
        UiMode.Material -> ProvidersScreenMaterial(state, actions)
        UiMode.Miuix -> ProvidersScreenMiuix(state, actions)
    }
}

@Composable
fun providerDisplayName(id: ProviderId): String {
    return when (id) {
        ProviderId.KERNEL_SU -> "KernelSU"
        ProviderId.KERNEL_SU_NEXT -> "KernelSU Next"
        ProviderId.MAGISK -> "Magisk"
        ProviderId.APATCH -> "APatch"
        ProviderId.BAKA_SU -> "BakaSU"
    }
}

@Composable
fun providerNote(id: ProviderId): String {
    return when (id) {
        ProviderId.KERNEL_SU -> stringResource(R.string.provider_note_ksu)
        ProviderId.KERNEL_SU_NEXT -> stringResource(R.string.provider_note_ksunext)
        ProviderId.MAGISK -> stringResource(R.string.provider_note_magisk)
        ProviderId.APATCH -> stringResource(R.string.provider_note_apatch)
        ProviderId.BAKA_SU -> stringResource(R.string.provider_note_bakasu)
    }
}

@Composable
fun providerCapabilityLabel(id: CapabilityId): String {
    return when (id) {
        CapabilityId.PROFILES -> stringResource(R.string.provider_cap_profiles)
        CapabilityId.MODULES -> stringResource(R.string.provider_cap_modules)
        CapabilityId.MODULE_REPO -> stringResource(R.string.provider_cap_module_repo)
        CapabilityId.BOOT_PATCHING -> stringResource(R.string.provider_cap_boot_patching)
        CapabilityId.SULOG -> stringResource(R.string.provider_cap_sulog)
        CapabilityId.HIDING -> stringResource(R.string.provider_cap_hiding)
        CapabilityId.LATE_LOAD -> stringResource(R.string.provider_cap_late_load)
        CapabilityId.SOFT_REBOOT -> stringResource(R.string.provider_cap_soft_reboot)
        CapabilityId.ZYGISK -> stringResource(R.string.provider_cap_zygisk)
        CapabilityId.KERNEL_MODULES -> stringResource(R.string.provider_cap_kernel_modules)
        CapabilityId.DYNAMIC_MANAGER -> stringResource(R.string.provider_cap_dynamic_manager)
        CapabilityId.RISK_SCANNER -> stringResource(R.string.provider_cap_risk_scanner)
        CapabilityId.SUSFS -> stringResource(R.string.provider_cap_susfs)
        CapabilityId.DENYLIST -> stringResource(R.string.provider_cap_denylist)
        CapabilityId.SU_DB -> stringResource(R.string.provider_cap_su_db)
    }
}
