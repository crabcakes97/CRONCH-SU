package me.weishu.kernelsu.ui.provider

import me.weishu.kernelsu.Natives

/**
 * The five root provider families — "The Five Perches" (NEW LORE: each
 * root family roosts on its own perch in the Coop; the Council only
 * trusts a perch it can see a bird on). The capability catalog below
 * is grounded in the upstream study notes in
 * `upstream-study/` (`*-STUDY.md`) — what each upstream supports, and what
 * Cronch SU Phase 1 actually drives. Nothing is flattened: a capability
 * an upstream has but Cronch SU cannot yet drive is UPSTREAM_ONLY, and
 * a capability an upstream does not have at all is UNSUPPORTED. No
 * provider is ever reported as working on the basis of a guess.
 *
 * License notes (from the study notes, read-only study, no code copied):
 * KernelSU / KernelSU-Next / Magisk / APatch managers are GPL-3.0;
 * BakaSU manager/userspace GPL-3.0-or-later, its kernel GPL-2.0-only;
 * KernelPatch (APatch's patch engine) GPL-2.0. Any future borrowing
 * must preserve notices and ship source under the same terms.
 */
enum class ProviderId {
    KERNEL_SU,
    KERNEL_SU_NEXT,
    MAGISK,
    APATCH,
    BAKA_SU,
}

/** What Phase 1 can honestly say about a provider on this phone. */
enum class DetectionState {
    /** A live probe answered: the provider is present. */
    DETECTED,

    /** A live probe ran and found nothing. */
    NOT_DETECTED,

    /** No probe is wired in Phase 1, so no claim is made either way. */
    DETECTION_UNAVAILABLE,
}

enum class CapabilityId {
    PROFILES,
    MODULES,
    MODULE_REPO,
    BOOT_PATCHING,
    SULOG,
    HIDING,
    LATE_LOAD,
    SOFT_REBOOT,
    ZYGISK,
    KERNEL_MODULES,
    DYNAMIC_MANAGER,
    RISK_SCANNER,
    SUSFS,
    DENYLIST,
    SU_DB,
}

/** How a capability stands in Cronch SU Phase 1. */
enum class CapabilityState {
    /** Cronch SU drives it today, for real. */
    INTEGRATED,

    /** The upstream supports it; Cronch SU does not drive it yet. */
    UPSTREAM_ONLY,

    /** The upstream does not support it (per the study evidence). */
    UNSUPPORTED,
}

data class ProviderCapability(
    val id: CapabilityId,
    val state: CapabilityState,
)

data class ProviderInfo(
    val id: ProviderId,
    val detection: DetectionState,
    /** What was actually observed, in plain words. Null when nothing was observed. */
    val evidence: String?,
    val capabilities: List<ProviderCapability>,
)

/** Raw probe results. All fields are observations, never assumptions. */
data class ProviderDetection(
    /** A KernelSU-family kernel answered the JNI probes (base or Next flavor). */
    val kernelSuFamilyDetected: Boolean,
    /** The Next-only probes answered with Next-specific values. */
    val isNextFlavor: Boolean,
    val kernelVersion: Int?,
    val kernelUapiVersion: Int?,
    val versionTag: String?,
    val hookMode: String?,
    val managerAppid: Int?,
    /** `which magisk` succeeded in a root shell. Presence only — says nothing about a running daemon. */
    val magiskBinaryPresent: Boolean,
)

/** The provider actually serving root right now, for the Home dashboard. */
data class ActiveProviderSummary(
    val provider: ProviderId?,
    val headline: String?,
    val detail: String?,
) {
    companion object {
        val None = ActiveProviderSummary(null, null, null)
    }
}

private fun cap(id: CapabilityId, state: CapabilityState) = ProviderCapability(id, state)

/**
 * The capability catalog. Sources: KernelSU-STUDY.md, KernelSU-Next-STUDY.md,
 * Magisk-STUDY.md, APatch-STUDY.md, BakaSU-STUDY.md (file:line evidence in
 * each note). KernelSU and KernelSU-Next share the wire protocol (identical
 * UAPI header), so the same Cronch SU code paths serve both — that is why
 * their INTEGRATED sets match except for Next-only extras, which stay
 * UPSTREAM_ONLY here (probed read-only, no controls wired in Phase 1).
 * SUSFS is deliberately UPSTREAM_ONLY for Next/BakaSU: it panics the test
 * phone, so Cronch SU must not expose a toggle for it.
 */
fun providerCatalog(detection: ProviderDetection): List<ProviderInfo> {
    val kernelSuEvidence = if (detection.kernelSuFamilyDetected) {
        buildString {
            append("KernelSU-family kernel")
            detection.kernelVersion?.let { append(" · version $it") }
            detection.kernelUapiVersion?.let { append(" · UAPI $it") }
            detection.versionTag?.takeIf { it != "Unknown" }?.let { append(" · tag $it") }
            detection.hookMode?.takeIf { it != "Unknown" }?.let { append(" · hook $it") }
        }
    } else {
        null
    }
    val kernelSuCapabilities = listOf(
        cap(CapabilityId.PROFILES, CapabilityState.INTEGRATED),
        cap(CapabilityId.MODULES, CapabilityState.INTEGRATED),
        cap(CapabilityId.MODULE_REPO, CapabilityState.INTEGRATED),
        cap(CapabilityId.BOOT_PATCHING, CapabilityState.INTEGRATED),
        cap(CapabilityId.SULOG, CapabilityState.INTEGRATED),
        cap(CapabilityId.HIDING, CapabilityState.INTEGRATED),
        cap(CapabilityId.LATE_LOAD, CapabilityState.INTEGRATED),
        cap(CapabilityId.SOFT_REBOOT, CapabilityState.INTEGRATED),
        cap(CapabilityId.ZYGISK, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.KERNEL_MODULES, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.DYNAMIC_MANAGER, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.RISK_SCANNER, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.SUSFS, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.DENYLIST, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.SU_DB, CapabilityState.UNSUPPORTED),
    )
    val nextCapabilities = listOf(
        cap(CapabilityId.PROFILES, CapabilityState.INTEGRATED),
        cap(CapabilityId.MODULES, CapabilityState.INTEGRATED),
        cap(CapabilityId.MODULE_REPO, CapabilityState.INTEGRATED),
        cap(CapabilityId.BOOT_PATCHING, CapabilityState.INTEGRATED),
        cap(CapabilityId.SULOG, CapabilityState.INTEGRATED),
        cap(CapabilityId.HIDING, CapabilityState.INTEGRATED),
        cap(CapabilityId.LATE_LOAD, CapabilityState.INTEGRATED),
        cap(CapabilityId.SOFT_REBOOT, CapabilityState.INTEGRATED),
        // Next-only extras: probed read-only in this tree, no controls wired.
        cap(CapabilityId.ZYGISK, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.DYNAMIC_MANAGER, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.RISK_SCANNER, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.SUSFS, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.KERNEL_MODULES, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.DENYLIST, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.SU_DB, CapabilityState.UNSUPPORTED),
    )
    val magiskCapabilities = listOf(
        cap(CapabilityId.PROFILES, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.SU_DB, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.MODULES, CapabilityState.UPSTREAM_ONLY),
        // No module repository in the Magisk tree: per-module updateJson only.
        cap(CapabilityId.MODULE_REPO, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.BOOT_PATCHING, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.SULOG, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.DENYLIST, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.ZYGISK, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.HIDING, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.LATE_LOAD, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.SOFT_REBOOT, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.KERNEL_MODULES, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.DYNAMIC_MANAGER, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.RISK_SCANNER, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.SUSFS, CapabilityState.UNSUPPORTED),
    )
    val apatchCapabilities = listOf(
        cap(CapabilityId.PROFILES, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.MODULES, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.MODULE_REPO, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.BOOT_PATCHING, CapabilityState.UPSTREAM_ONLY),
        // No audit-log surface was found in the APatch study; recheck in Phase 2.
        cap(CapabilityId.SULOG, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.HIDING, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.LATE_LOAD, CapabilityState.UPSTREAM_ONLY),
        // KPM kernel modules are a separate APatch-only format: never merge with APM.
        cap(CapabilityId.KERNEL_MODULES, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.SOFT_REBOOT, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.ZYGISK, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.DYNAMIC_MANAGER, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.RISK_SCANNER, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.SUSFS, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.DENYLIST, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.SU_DB, CapabilityState.UNSUPPORTED),
    )
    val bakasuCapabilities = listOf(
        cap(CapabilityId.PROFILES, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.MODULES, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.MODULE_REPO, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.BOOT_PATCHING, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.SULOG, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.HIDING, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.LATE_LOAD, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.SOFT_REBOOT, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.ZYGISK, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.DYNAMIC_MANAGER, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.RISK_SCANNER, CapabilityState.UPSTREAM_ONLY),
        cap(CapabilityId.SUSFS, CapabilityState.UPSTREAM_ONLY),
        // KPM support was dropped in BakaSU (study §1.4: dropped at 34944).
        cap(CapabilityId.KERNEL_MODULES, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.DENYLIST, CapabilityState.UNSUPPORTED),
        cap(CapabilityId.SU_DB, CapabilityState.UNSUPPORTED),
    )
    return listOf(
        ProviderInfo(
            id = ProviderId.KERNEL_SU,
            // Next IS KernelSU-family: when a Next kernel is running, this
            // perch is genuinely occupied too — the Next entry refines it.
            detection = if (detection.kernelSuFamilyDetected) DetectionState.DETECTED else DetectionState.NOT_DETECTED,
            evidence = kernelSuEvidence,
            capabilities = kernelSuCapabilities,
        ),
        ProviderInfo(
            id = ProviderId.KERNEL_SU_NEXT,
            detection = if (detection.isNextFlavor) DetectionState.DETECTED else DetectionState.NOT_DETECTED,
            evidence = if (detection.isNextFlavor) kernelSuEvidence else null,
            capabilities = nextCapabilities,
        ),
        ProviderInfo(
            id = ProviderId.MAGISK,
            detection = if (detection.magiskBinaryPresent) DetectionState.DETECTED else DetectionState.NOT_DETECTED,
            evidence = if (detection.magiskBinaryPresent) "magisk binary answered in a root shell" else null,
            capabilities = magiskCapabilities,
        ),
        ProviderInfo(
            id = ProviderId.APATCH,
            detection = DetectionState.DETECTION_UNAVAILABLE,
            evidence = null,
            capabilities = apatchCapabilities,
        ),
        ProviderInfo(
            id = ProviderId.BAKA_SU,
            detection = DetectionState.DETECTION_UNAVAILABLE,
            evidence = null,
            capabilities = bakasuCapabilities,
        ),
    )
}

/**
 * Classifies the live kernel from the JNI probes. Pure: no I/O, safe to
 * call from [me.weishu.kernelsu.ui.viewmodel.HomeViewModel.buildState].
 *
 * Flavor rule (from the JNI graceful-degradation contract in Natives.kt):
 * on a base kernel the Next probes answer "Unknown"/0/false. A version tag
 * containing "next" proves the Next flavor; any other real tag is reported
 * verbatim and classified as KernelSU-family — BakaSU shares this wire
 * protocol, so a BakaSU tag must never be relabeled as something else.
 * A real hook mode with no usable tag still proves the Next flavor,
 * because the hook-mode probe exists only on Next-family kernels.
 */
fun summarizeKernelSide(
    isManager: Boolean,
    ksuVersion: Int?,
    kernelUapiVersion: Int?,
    versionTag: String?,
    hookMode: String?,
    managerAppid: Int?,
): ActiveProviderSummary {
    if (!isManager || ksuVersion == null || ksuVersion < Natives.MINIMAL_SUPPORTED_KERNEL) {
        return ActiveProviderSummary.None
    }
    val realTag = versionTag?.takeIf { it.isNotBlank() && it != "Unknown" }
    val realHook = hookMode?.takeIf { it.isNotBlank() && it != "Unknown" }
    val isNext = realTag?.contains("next", ignoreCase = true) == true ||
        (realTag == null && realHook != null)
    val detail = buildString {
        append("kernel $ksuVersion")
        kernelUapiVersion?.let { append(" · UAPI $it") }
        realTag?.let { append(" · tag $it") }
        realHook?.let { append(" · hook $it") }
        managerAppid?.takeIf { it != 0 }?.let { append(" · manager app id $it") }
    }
    return if (isNext) {
        ActiveProviderSummary(ProviderId.KERNEL_SU_NEXT, "KernelSU Next", detail)
    } else {
        ActiveProviderSummary(ProviderId.KERNEL_SU, "KernelSU", detail)
    }
}
