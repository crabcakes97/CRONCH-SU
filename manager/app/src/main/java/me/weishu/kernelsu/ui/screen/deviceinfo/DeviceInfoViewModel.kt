package me.weishu.kernelsu.ui.screen.deviceinfo

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.ksuApp
import me.weishu.kernelsu.ui.util.CronchLog
import java.io.File

/**
 * Cronch SU live device info. Every value is read from a real system API at
 * refresh time — ActivityManager for RAM, /proc/stat for CPU, StatFs for
 * storage, Build for device identity, the sticky battery intent for charge
 * state, and the same Natives getters Home uses for root status. Nothing is
 * hardcoded or estimated; a value that cannot be read is null and renders as
 * "Unavailable" with the refresh timestamp shown honestly.
 */
data class DeviceInfoUiState(
    val isRefreshing: Boolean = false,
    val lastUpdatedAt: Long = 0L,
    val ramTotalBytes: Long? = null,
    val ramAvailableBytes: Long? = null,
    val ramThresholdBytes: Long? = null,
    val lowMemory: Boolean = false,
    val cpuUsagePercent: Float? = null,
    val cpuModel: String? = null,
    val cpuCores: Int = Runtime.getRuntime().availableProcessors(),
    val uptimeMillis: Long = 0L,
    val storageTotalBytes: Long = 0L,
    val storageFreeBytes: Long = 0L,
    val deviceModel: String = Build.MODEL ?: "",
    val deviceCodename: String = Build.DEVICE ?: "",
    val board: String = Build.BOARD ?: "",
    val hardware: String = Build.HARDWARE ?: "",
    val kernelVersion: String = System.getProperty("os.version") ?: "",
    val ksuWorking: Boolean = false,
    val ksuVersion: Int? = null,
    val lkmMode: Boolean? = null,
    val safeMode: Boolean = false,
    val lateLoadMode: Boolean = false,
    val batteryPercent: Int? = null,
    val batteryCharging: Boolean? = null,
    // KernelSU-Next read-only probes. The section renders only when at
    // least one probe returns an informative (non-sentinel) value.
    val hookMode: String? = null,
    val versionTag: String? = null,
    val managerAppid: Int = 0,
    val zygiskEnabled: Boolean = false,
    val avcSpoofOn: Boolean = false,
) {
    val showNextInfo: Boolean
        get() = (hookMode != null && hookMode != "Unknown") ||
            (versionTag != null && versionTag != "Unknown") ||
            managerAppid != 0 || zygiskEnabled || avcSpoofOn
    val ramUsedBytes: Long?
        get() = if (ramTotalBytes != null && ramAvailableBytes != null) {
            (ramTotalBytes - ramAvailableBytes).coerceAtLeast(0L)
        } else {
            null
        }
    val ramUsageFraction: Float?
        get() {
            val used = ramUsedBytes
            return if (ramTotalBytes != null && ramTotalBytes > 0 && used != null) {
                used.toFloat() / ramTotalBytes.toFloat()
            } else {
                null
            }
        }
    val storageUsageFraction: Float
        get() = if (storageTotalBytes > 0) {
            (storageTotalBytes - storageFreeBytes).toFloat() / storageTotalBytes.toFloat()
        } else {
            0f
        }
}

fun formatBytes(bytes: Long): String {
    val gib = bytes.toDouble() / (1024 * 1024 * 1024)
    return if (gib >= 1.0) {
        "%.2f GiB".format(gib)
    } else {
        "%.0f MiB".format(bytes.toDouble() / (1024 * 1024))
    }
}

fun formatUptime(millis: Long): String {
    val totalSeconds = millis / 1000
    val days = totalSeconds / 86400
    val hours = (totalSeconds % 86400) / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (days > 0) {
        "%dd %02dh %02dm".format(days, hours, minutes)
    } else {
        "%02dh:%02dm:%02ds".format(hours, minutes, seconds)
    }
}

class DeviceInfoViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DeviceInfoUiState())
    val uiState: StateFlow<DeviceInfoUiState> = _uiState.asStateFlow()
    private val refreshMutex = Mutex()

    // Last probe signature written to the Coop log. The snapshot refreshes
    // every 3 s while visible, so probes are logged only when their values
    // actually change — otherwise the log would drown in repeats.
    private var lastProbeSignature: String? = null

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            if (!refreshMutex.tryLock()) return@launch
            try {
                _uiState.update { it.copy(isRefreshing = true) }
                val snapshot = withContext(Dispatchers.IO) { readSnapshot() }
                val probeSignature = "hook=${snapshot.hookMode ?: "unavailable"} " +
                    "tag=${snapshot.versionTag ?: "unavailable"} " +
                    "appid=${snapshot.managerAppid} " +
                    "zygisk=${snapshot.zygiskEnabled} avcSpoof=${snapshot.avcSpoofOn}"
                if (probeSignature != lastProbeSignature) {
                    lastProbeSignature = probeSignature
                    CronchLog.log("Next", "probes: $probeSignature")
                }
                _uiState.update {
                    it.copy(
                        isRefreshing = false,
                        lastUpdatedAt = System.currentTimeMillis(),
                        ramTotalBytes = snapshot.ramTotalBytes,
                        ramAvailableBytes = snapshot.ramAvailableBytes,
                        ramThresholdBytes = snapshot.ramThresholdBytes,
                        lowMemory = snapshot.lowMemory,
                        cpuUsagePercent = snapshot.cpuUsagePercent,
                        cpuModel = snapshot.cpuModel,
                        uptimeMillis = snapshot.uptimeMillis,
                        storageTotalBytes = snapshot.storageTotalBytes,
                        storageFreeBytes = snapshot.storageFreeBytes,
                        ksuWorking = snapshot.ksuWorking,
                        ksuVersion = snapshot.ksuVersion,
                        lkmMode = snapshot.lkmMode,
                        safeMode = snapshot.safeMode,
                        lateLoadMode = snapshot.lateLoadMode,
                        batteryPercent = snapshot.batteryPercent,
                        batteryCharging = snapshot.batteryCharging,
                        hookMode = snapshot.hookMode,
                        versionTag = snapshot.versionTag,
                        managerAppid = snapshot.managerAppid,
                        zygiskEnabled = snapshot.zygiskEnabled,
                        avcSpoofOn = snapshot.avcSpoofOn,
                    )
                }
            } finally {
                refreshMutex.unlock()
            }
        }
    }

    /** Plain-text export of the current snapshot, same values as on screen. */
    fun buildDeviceInfoExport(): String {
        val s = _uiState.value
        val date = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm",
            java.util.Locale.getDefault(),
        ).format(java.util.Date(s.lastUpdatedAt))
        val unavailable = "Unavailable"
        return buildString {
            appendLine("Cronch SU - Device Info export")
            appendLine("Generated: $date")
            appendLine()
            appendLine("Root status: ${if (s.ksuWorking) "working (version ${s.ksuVersion ?: unavailable})" else "not working"}")
            appendLine("LKM mode: ${s.lkmMode ?: unavailable}")
            appendLine("Safe mode: ${s.safeMode}")
            appendLine("Late-load mode: ${s.lateLoadMode}")
            appendLine()
            appendLine("RAM total: ${s.ramTotalBytes?.let(::formatBytes) ?: unavailable}")
            appendLine("RAM available: ${s.ramAvailableBytes?.let(::formatBytes) ?: unavailable}")
            appendLine("RAM used: ${s.ramUsedBytes?.let(::formatBytes) ?: unavailable}")
            appendLine("RAM usage: ${s.ramUsageFraction?.let { "%.0f%%".format(it * 100) } ?: unavailable}")
            appendLine("Low memory: ${s.lowMemory}")
            appendLine()
            appendLine("CPU usage: ${s.cpuUsagePercent?.let { "%.0f%%".format(it) } ?: unavailable}")
            appendLine("CPU: ${s.cpuModel ?: unavailable} (${s.cpuCores} cores)")
            appendLine("Uptime: ${formatUptime(s.uptimeMillis)}")
            appendLine()
            appendLine("Device model: ${s.deviceModel}")
            appendLine("Codename: ${s.deviceCodename}")
            appendLine("Board: ${s.board}")
            appendLine("Hardware: ${s.hardware}")
            appendLine("Kernel version: ${s.kernelVersion}")
            appendLine()
            appendLine("Storage (/data) total: ${formatBytes(s.storageTotalBytes)}")
            appendLine("Storage (/data) free: ${formatBytes(s.storageFreeBytes)}")
            appendLine("Battery: ${s.batteryPercent?.let { "$it%" } ?: unavailable} (${if (s.batteryCharging == true) "charging" else "not charging"})")
            if (s.showNextInfo) {
                appendLine()
                appendLine("KernelSU-Next info (read-only probes):")
                appendLine("Hook mode: ${s.hookMode ?: unavailable}")
                appendLine("Version tag: ${s.versionTag ?: unavailable}")
                appendLine("Manager app ID: ${s.managerAppid}")
                appendLine("Zygisk: ${s.zygiskEnabled}")
                appendLine("AVC spoof: ${s.avcSpoofOn}")
            }
        }
    }

    private data class Snapshot(
        val ramTotalBytes: Long?,
        val ramAvailableBytes: Long?,
        val ramThresholdBytes: Long?,
        val lowMemory: Boolean,
        val cpuUsagePercent: Float?,
        val cpuModel: String?,
        val uptimeMillis: Long,
        val storageTotalBytes: Long,
        val storageFreeBytes: Long,
        val ksuWorking: Boolean,
        val ksuVersion: Int?,
        val lkmMode: Boolean?,
        val safeMode: Boolean,
        val lateLoadMode: Boolean,
        val batteryPercent: Int?,
        val batteryCharging: Boolean?,
        val hookMode: String?,
        val versionTag: String?,
        val managerAppid: Int,
        val zygiskEnabled: Boolean,
        val avcSpoofOn: Boolean,
    )

    private suspend fun readSnapshot(): Snapshot {
        // RAM — ActivityManager.MemoryInfo is the platform source of truth.
        var ramTotal: Long? = null
        var ramAvail: Long? = null
        var ramThreshold: Long? = null
        var lowMem = false
        try {
            val am = ksuApp.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            ramTotal = info.totalMem
            ramAvail = info.availMem
            ramThreshold = info.threshold
            lowMem = info.lowMemory
        } catch (e: Exception) {
            Log.w(TAG, "MemoryInfo read failed", e)
        }

        // CPU usage — two /proc/stat samples 400 ms apart, real interval %.
        val cpuUsage = try {
            val first = readCpuTimes()
            delay(400)
            val second = readCpuTimes()
            if (first != null && second != null && second.total > first.total) {
                val idleDelta = second.idle - first.idle
                val totalDelta = second.total - first.total
                ((1.0 - idleDelta.toDouble() / totalDelta.toDouble()) * 100).toFloat()
                    .coerceIn(0f, 100f)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "CPU sample failed", e)
            null
        }
        val cpuModel = readCpuModel()

        // Storage — StatFs on the data partition.
        var storageTotal = 0L
        var storageFree = 0L
        try {
            val stat = StatFs(Environment.getDataDirectory().path)
            storageTotal = stat.blockCountLong * stat.blockSizeLong
            storageFree = stat.availableBlocksLong * stat.blockSizeLong
        } catch (e: Exception) {
            Log.w(TAG, "StatFs failed", e)
        }

        // Root status — same Natives getters Home uses.
        val isManager = try { Natives.isManager } catch (e: Exception) { false }
        val ksuVersion = try {
            if (isManager) Natives.version else null
        } catch (e: Exception) {
            null
        }
        val lkmMode = try {
            if (isManager) Natives.isLkmMode else null
        } catch (e: Exception) {
            null
        }
        val safeMode = try { Natives.isSafeMode } catch (e: Exception) { false }
        val lateLoad = try { Natives.isLateLoadMode } catch (e: Exception) { false }

        // Battery — sticky intent, no receiver registration needed.
        var batteryPercent: Int? = null
        var batteryCharging: Boolean? = null
        try {
            val intent = ksuApp.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            )
            if (intent != null) {
                val level = intent.getIntExtra("level", -1)
                val scale = intent.getIntExtra("scale", -1)
                if (level >= 0 && scale > 0) {
                    batteryPercent = (level * 100 / scale)
                }
                val plugged = intent.getIntExtra("plugged", -1)
                batteryCharging = plugged != 0 && plugged != -1
            }
        } catch (e: Exception) {
            Log.w(TAG, "Battery read failed", e)
        }

        // KernelSU-Next read-only probes. Each call degrades gracefully on
        // a base kernel (sentinel values), so a failure here never breaks
        // the rest of the snapshot.
        val hookMode = runCatching { Natives.getHookMode() }.getOrNull()
        val versionTag = runCatching { Natives.getVersionTag() }.getOrNull()
        val managerAppid = runCatching { Natives.getManagerAppid() }.getOrDefault(0)
        val zygiskEnabled = runCatching { Natives.isZygiskEnabled() }.getOrDefault(false)
        val avcSpoofOn = runCatching { Natives.isAvcSpoofEnabled() }.getOrDefault(false)

        return Snapshot(
            ramTotalBytes = ramTotal,
            ramAvailableBytes = ramAvail,
            ramThresholdBytes = ramThreshold,
            lowMemory = lowMem,
            cpuUsagePercent = cpuUsage,
            cpuModel = cpuModel,
            uptimeMillis = SystemClock.elapsedRealtime(),
            storageTotalBytes = storageTotal,
            storageFreeBytes = storageFree,
            ksuWorking = isManager,
            ksuVersion = ksuVersion,
            lkmMode = lkmMode,
            safeMode = safeMode,
            lateLoadMode = lateLoad,
            batteryPercent = batteryPercent,
            batteryCharging = batteryCharging,
            hookMode = hookMode,
            versionTag = versionTag,
            managerAppid = managerAppid,
            zygiskEnabled = zygiskEnabled,
            avcSpoofOn = avcSpoofOn,
        )
    }

    private data class CpuTimes(val idle: Long, val total: Long)

    private fun readCpuTimes(): CpuTimes? {
        return try {
            val line = File("/proc/stat").bufferedReader().useLines { lines ->
                lines.firstOrNull { it.startsWith("cpu ") }
            } ?: return null
            val fields = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
            if (fields.size < 4) return null
            val idle = fields[3] + if (fields.size > 4) fields[4] else 0L
            CpuTimes(idle = idle, total = fields.sum())
        } catch (e: Exception) {
            Log.w(TAG, "readCpuTimes failed", e)
            null
        }
    }

    private fun readCpuModel(): String? {
        return try {
            File("/proc/cpuinfo").bufferedReader().useLines { lines ->
                for (line in lines) {
                    if (line.startsWith("model name")) {
                        return@useLines line.substringAfter(":", "").trim().ifEmpty { null }
                    }
                }
                null
            } ?: try {
                File("/proc/cpuinfo").bufferedReader().useLines { lines ->
                    lines.firstOrNull { it.startsWith("Hardware") }
                        ?.substringAfter(":", "")?.trim()?.ifEmpty { null }
                }
            } catch (e: Exception) {
                null
            }
        } catch (e: Exception) {
            Log.w(TAG, "readCpuModel failed", e)
            null
        }
    }

    private companion object {
        const val TAG = "DeviceInfoViewModel"
    }
}
