package me.weishu.kernelsu.ui.provider

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.ui.util.hasMagisk

/**
 * Runs the Phase 1 provider probes. Every probe is wrapped in runCatching:
 * a probe that throws (no JNI, no root shell, no daemon) reads as
 * "not detected", never as a crash and never as a guessed positive.
 *
 * What is real in Phase 1:
 * - KernelSU / KernelSU Next: the JNI probes in [Natives] (kernel version,
 *   crown, UAPI version, Next-only hook mode / version tag / manager app id).
 * - Magisk: presence only, via [hasMagisk] (`which magisk` in a root shell).
 *   A present binary is NOT a running daemon and grants no control.
 * What is NOT probed in Phase 1 (reads DETECTION_UNAVAILABLE, honestly):
 * - APatch: would need a supercall/SuperKey probe — Phase 2+ work.
 * - BakaSU: shares the KernelSU wire protocol; no BakaSU-specific probe
 *   is wired, so a BakaSU kernel surfaces under the KernelSU-family entry.
 */
object ProviderDetector {

    suspend fun detect(): ProviderDetection = withContext(Dispatchers.IO) {
        val isManager = runCatching { Natives.isManager }.getOrDefault(false)
        val kernelVersion = runCatching { if (isManager) Natives.version else null }.getOrNull()
        val kernelUapiVersion = runCatching { if (isManager) Natives.kernelUAPIVersion else null }.getOrNull()
        val versionTag = runCatching { Natives.getVersionTag() }.getOrNull()
        val hookMode = runCatching { Natives.getHookMode() }.getOrNull()
        val managerAppid = runCatching { Natives.getManagerAppid() }.getOrNull()
        val magiskBinaryPresent = runCatching { hasMagisk() }.getOrDefault(false)

        val familyDetected = isManager &&
            kernelVersion != null &&
            kernelVersion >= Natives.MINIMAL_SUPPORTED_KERNEL
        val realTag = versionTag?.takeIf { it.isNotBlank() && it != "Unknown" }
        val realHook = hookMode?.takeIf { it.isNotBlank() && it != "Unknown" }
        val isNext = familyDetected &&
            (realTag?.contains("next", ignoreCase = true) == true || (realTag == null && realHook != null))

        ProviderDetection(
            kernelSuFamilyDetected = familyDetected,
            isNextFlavor = isNext,
            kernelVersion = kernelVersion,
            kernelUapiVersion = kernelUapiVersion,
            versionTag = versionTag,
            hookMode = hookMode,
            managerAppid = managerAppid,
            magiskBinaryPresent = magiskBinaryPresent,
        )
    }

    /** The Home dashboard summary, derived from one detection result. */
    fun summarize(detection: ProviderDetection): ActiveProviderSummary {
        return summarizeKernelSide(
            isManager = detection.kernelSuFamilyDetected,
            ksuVersion = detection.kernelVersion,
            kernelUapiVersion = detection.kernelUapiVersion,
            versionTag = detection.versionTag,
            hookMode = detection.hookMode,
            managerAppid = detection.managerAppid,
        )
    }
}
