package me.weishu.kernelsu.ui.screen.deviceinfo

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode

data class DeviceInfoRow(val label: String, val value: String)

/** Optional tappable credit link rendered under a section's rows. */
data class DeviceInfoLink(val label: String, val url: String)

data class DeviceInfoSection(
    val title: String,
    val rows: List<DeviceInfoRow>,
    val link: DeviceInfoLink? = null,
)

/**
 * Shared section/row model for both UI variants. Every value comes from the
 * live ViewModel snapshot; unreadable values render as "Unavailable" and the
 * screen always shows how old the snapshot is.
 */
@Composable
fun deviceInfoSections(state: DeviceInfoUiState): List<DeviceInfoSection> {
    val unavailable = stringResource(R.string.device_info_unavailable)
    val yes = stringResource(R.string.device_info_yes)
    val no = stringResource(R.string.device_info_no)
    val charging = stringResource(R.string.device_info_battery_charging)
    val notCharging = stringResource(R.string.device_info_battery_not_charging)
    val sections = mutableListOf(
        DeviceInfoSection(
            title = stringResource(R.string.device_info_root_status),
            rows = listOf(
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_root_status),
                    value = if (state.ksuWorking) {
                        stringResource(R.string.home_working) + (state.ksuVersion?.let { " (v$it)" } ?: "")
                    } else {
                        stringResource(R.string.home_not_installed)
                    },
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_lkm),
                    value = state.lkmMode?.let { if (it) yes else no } ?: unavailable,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_safe_mode),
                    value = if (state.safeMode) yes else no,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_late_load),
                    value = if (state.lateLoadMode) yes else no,
                ),
            ),
        ),
        DeviceInfoSection(
            title = stringResource(R.string.device_info_ram),
            rows = listOf(
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_ram_total),
                    value = state.ramTotalBytes?.let(::formatBytes) ?: unavailable,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_ram_available),
                    value = state.ramAvailableBytes?.let(::formatBytes) ?: unavailable,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_ram_used),
                    value = state.ramUsedBytes?.let(::formatBytes) ?: unavailable,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_ram_usage),
                    value = state.ramUsageFraction?.let { "%.0f%%".format(it * 100) } ?: unavailable,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_low_memory),
                    value = if (state.lowMemory) yes else no,
                ),
            ),
        ),
        DeviceInfoSection(
            title = stringResource(R.string.device_info_cpu),
            rows = listOf(
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_cpu),
                    value = state.cpuUsagePercent?.let { "%.0f%%".format(it) } ?: unavailable,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_cpu_model),
                    value = state.cpuModel ?: unavailable,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_cores),
                    value = state.cpuCores.toString(),
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_uptime),
                    value = formatUptime(state.uptimeMillis),
                ),
            ),
        ),
        DeviceInfoSection(
            title = stringResource(R.string.device_info_model),
            rows = listOf(
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_model),
                    value = state.deviceModel,
                ),
                DeviceInfoRow(label = stringResource(R.string.device_info_codename), value = state.deviceCodename),
                DeviceInfoRow(label = stringResource(R.string.device_info_board), value = state.board),
                DeviceInfoRow(label = stringResource(R.string.device_info_hardware), value = state.hardware),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_kernel),
                    value = state.kernelVersion.ifEmpty { unavailable },
                ),
            ),
        ),
        DeviceInfoSection(
            title = stringResource(R.string.device_info_storage),
            rows = listOf(
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_storage),
                    value = formatBytes(state.storageTotalBytes),
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_storage_free),
                    value = formatBytes(state.storageFreeBytes),
                ),
            ),
        ),
        DeviceInfoSection(
            title = stringResource(R.string.device_info_battery),
            rows = listOf(
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_battery),
                    value = state.batteryPercent?.let { "$it%" } ?: unavailable,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_battery_state),
                    value = when (state.batteryCharging) {
                        true -> charging
                        false -> notCharging
                        null -> unavailable
                    },
                ),
            ),
        ),
    )
    // KernelSU-Next read-only probes: shown only when at least one probe
    // returned an informative value (never on a base kernel, where every
    // probe reports its "unknown" sentinel).
    if (state.showNextInfo) {
        sections += DeviceInfoSection(
            title = stringResource(R.string.device_info_next_title),
            rows = listOf(
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_hook_mode),
                    value = state.hookMode ?: unavailable,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_version_tag),
                    value = state.versionTag ?: unavailable,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_manager_appid),
                    value = state.managerAppid.toString(),
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_zygisk),
                    value = if (state.zygiskEnabled) yes else no,
                ),
                DeviceInfoRow(
                    label = stringResource(R.string.device_info_avc_spoof),
                    value = if (state.avcSpoofOn) yes else stringResource(R.string.device_info_avc_spoof_off),
                ),
            ),
            // Tappable credit: opens the KernelSU-Next project in the
            // browser. No plain-text URL — the row itself is the link.
            link = DeviceInfoLink(
                label = stringResource(R.string.device_info_next_credit),
                url = "https://github.com/KernelSU-Next/KernelSU-Next",
            ),
        )
    }
    return sections
}

@Composable
fun DeviceInfoScreen() {
    when (LocalUiMode.current) {
        UiMode.Miuix -> DeviceInfoMiuix()
        UiMode.Material -> DeviceInfoMaterial()
    }
}
