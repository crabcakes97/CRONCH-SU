# Cronch SU

A lore rebrand of the KernelSU manager app. The root mechanism is untouched:
same kernel driver contract, same su protocol, same daemon handshake, same
module install/mount logic, same manager <-> daemon identification.

## Upstream

- Base: tiann/KernelSU @ 75e179d378d7aaa0de8946358a3384444c3e36b0
  ("update ads.txt"). All upstream credits stay intact; the About screen
  still links tiann's GitHub and Telegram.
- KernelSU-Next @ b427a3f9c2421a443f961007bfce66b25486643f (read-only
  probes only, see below). KernelSU-Next is credited in the Device Info
  screen section that surfaces those probes.

## Identity decision: package name KEPT

`applicationId` stays `me.weishu.kernelsu`, and the Kotlin package/class
names are unchanged. Evidence (all verified in-tree):

1. `kernel/manager/throne_tracker.c`: `crown_manager()` matches the package
   name against the compile-time `KSU_PACKAGE_NAME` (default
   `me.weishu.kernelsu`), and crowning requires the signature check
   (`is_manager_apk()` -> `check_v2_signature` against `EXPECTED_SIZE` /
   `EXPECTED_HASH` in `kernel/manager/apk_sign.c`) to pass first.
2. `userspace/ksud/src/late_load.rs`: late-load force-stops/starts
   `{package}/me.weishu.kernelsu.ui.MainActivity` — the component class
   path is hardcoded in the prebuilt daemon.
3. The manager build supports rebranding by property: `-PKSU_NAME`
   becomes `app_name` via resValue. The official rebrand mechanism is the
   NAME property with the package kept.

Consequences: a debug-signed Cronch SU cannot update over the release
manager (signature mismatch — uninstall the old manager first), and a
stock tiann kernel will not crown a debug-signed manager (signature
check). Behavior against a KernelSU-Next LKM setup is untested.

## What changed (manager app only)

- App label: `Cronch SU` (via `-PKSU_NAME="Cronch SU"`; no `app_name`
  string exists in resources).
- Strings: nav labels, home/module/settings/about headers, empty states,
  and support text use established CRONCH lore only (Poultry Council,
  Four Pillars with exact emoji, flavor archive names, the six Official
  Rituals, the five given rap-roster names). Safety warnings, profile
  allow/deny/ask semantics, and all functional text are unchanged.
  Upstream "KernelSU" mentions remain only where they credit upstream.
- Launcher icon: same vector geometry, foreground recolored Dorito
  orange (`#FFE8590C`), splash background ranch cream.
- Theme engine (`ui/theme/CronchThemes.kt`): 20 presets = 10 archive
  flavors x Day / After Dark, each a (seed, palette style, color mode)
  applied through the existing `color_mode` / `key_color` / `color_style`
  prefs, so the whole app follows the theme instantly with no restart.
  Custom accent color picker (RGB sliders + validated hex + live preview
  rendered with the real scheme generator) on the Color Palette screen in
  both UI modes; dark variants + custom colors included; reset-to-default
  (keyColor 0 = system dynamic color) included.
- Animations: the existing theme-change color animation
  (`ColorScheme.animateAsState`) is kept and now respects the system
  animator-duration-scale setting (no animation when the user disables
  animations). Screen transitions and press feedback are the platform /
  Material / Miuix defaults.
- Live Device Info screen (Settings -> Device Info, both UI modes):
  RAM (ActivityManager), CPU usage (/proc/stat interval sample) + model
  (/proc/cpuinfo), uptime, storage (StatFs /data), device identity
  (Build), kernel version, battery (sticky intent), and root status
  (same Natives getters Home uses). Refreshes every 3 s while visible,
  shows snapshot age honestly, copy + file-picker export. Unreadable
  values render as "Unavailable", never invented.
- Module status filter (Flavor Stash, both UI modes): All / Enabled /
  Disabled chips, applied in ModuleViewModel to both the plain list and
  search results.
- Pecking Order export (Superuser, both UI modes): file-picker export
  of the live superuser list (label, package, uid, real allow/deny
  state) as plain text.
- About screens: one lore/credit line (`about_cronch_note`).
- Coop log console (home screen, both UI modes, first item below the top
  bar): a verbose, scrolling log of everything the app is doing, with
  real timestamps and real event messages only. Implementation:
  `ui/util/CronchLog.kt` (bounded ring buffer, last 500 lines, thread-safe,
  failure-proof logging calls) + `ui/screen/home/CronchLogConsole.kt`
  (240 dp console, auto-follows new lines, pauses when the user scrolls
  up, "Latest" jump button, Clear + Copy header actions). Rainbow
  lettering is the default (hue rotates per line, lightness chosen for
  readability on light, dark, and AMOLED themes; the log background stays
  a plain theme surface); a persisted toggle (`rainbowLog` pref, default
  on) switches it off. Wired events: app launch, root status checks and
  update checks (HomeViewModel), module enable/disable/uninstall/undo
  (ModuleViewModel), module installs and boot flash/restore/uninstall
  (FlashUtils), superuser profile saves and refusals (AppProfileScreen),
  theme/color and feature-toggle changes (SettingsViewModel),
  KernelSU-Next probe results on change only (DeviceInfoViewModel),
  jailbreak request/timeout (HomeScreen).
- Credits (About screen, both UI modes, appended after the existing
  upstream links — nothing removed): fork contributor crabcakes97 with
  the tappable GitHub link https://github.com/crabcakes97, and official
  KernelSU by tiann with the tappable link
  https://github.com/tiann/KernelSU. Every credit row opens its URL in
  the browser via the same uri handler as the existing tiann links; no
  plain-text URLs. The KernelSU-Next credit in the Device Info section
  is likewise a tappable row opening
  https://github.com/KernelSU-Next/KernelSU-Next.
- Serious note (About screen, both UI modes, below the links — the ONLY
  serious-toned added section; all other added content stays lore/absurd
  and no other added content was re-toned): "Serious note: Magisk Next
  is Kyle's secret project — kernel-based Magisk. Shout-out to
  topjohnwu, the creator of Magisk. Magisk Next will be compatible with
  DirtyFrag soon." Kyle's own project statement, no link (no repo named),
  no extra claims. The word "GOAT" appears nowhere in the app.
- Chainfire tribute (About screen, both UI modes, below the Magisk Next
  note, in the credits area with the other shout-outs): "Tribute to
  Chainfire — the OG. Jorrit Jongma, XDA Senior Recognized Developer,
  creator of SuperSU, CF-Auto-Root, Triangle Away, CF.lumen, Mobile
  ODIN, FlashFire (spiritual successor to Mobile ODIN), StickMount,
  suhide, 500 Firepaper, DSLR Controller, LiveBoot, and Sideload
  Launcher for Android TV." Text tribute only — no link (none was
  supplied and none was invented); the app list is exactly the verified
  list, nothing added. The Magisk Next note stays the single
  serious PROJECT note.

## KernelSU-Next: what was carried, what was left out

Carried (additive, capability-gated, base behavior preserved):

- Five read-only probes, ported from KernelSU-Next's own
  graceful-failure implementations (`manager/app/src/main/cpp/ksu.cc`,
  `ksu.h`, `uapi/supercall.h` ioctls 98/99, `uapi/feature.h`
  `KSU_FEATURE_AVC_SPOOF = 10003`, `jni.cc`, `Natives.kt`):
  `getHookMode`, `getVersionTag`, `getManagerAppid`, `isZygiskEnabled`,
  `isAvcSpoofEnabled`. On a base kernel every probe returns its
  documented sentinel ("Unknown" / 0 / false) and nothing changes.
- Device Info shows a "KernelSU-Next info" section ONLY when at least
  one probe returns an informative value. Base `MINIMAL_SUPPORTED_KERNEL`
  (32513) is unchanged; Next's 33188 requirement was NOT adopted.

Left out, with reasons:

- SUSFS guard LKM loading (Next Settings): driver-side; needs the
  susfs_guard LKM + Next ksud module-load path. No such path exists in
  the base manager, and loading kernel modules is root-mechanism work.
- Adb-root Natives getters/setters: base already covers adb root via
  ksud feature flags (SettingsViewModel); a second mechanism risks
  conflicting state.
- `allowList` getter: deprecated in KernelSU-Next itself.
- Next UI screens (AppLock, biometric auth, BackupRestore,
  Customization, Developer, MetaModule): Next's manager is a different,
  single-variant UI in package `com.rifsxd.ksunext`; porting its screens
  would replace base UI, which the additive rule forbids.
- Profile/root-mode UI differences: the `Natives.Profile` struct is
  byte-identical between base and Next (verified by diff) — no kernel
  profile ABI to carry; base profile UI already covers the same fields.
- Next ksud/userspace + kernel driver changes: out of scope. This is a
  manager-APK fork; the kernel and daemon are untouched.

## What stayed untouched

`kernel/`, `userspace/`, `website/`, `js/`, `scripts/`, `risk/`,
`docs/`, build scripts, the su protocol, the module lifecycle, the
allowlist/profile ABI, `MINIMAL_SUPPORTED_KERNEL`, and every manager
<-> daemon identification path. Kotlin package and class names are
unchanged; only user-visible literals were rebranded.

## Build status (2026-10-31, honest)

- ksud: BUILT. `cargo build --target aarch64-linux-android --release`
  with NDK 29 `aarch64-linux-android31-clang` wrappers (cargo-ndk 4.1.2
  panics on NDK 29's unified sysroot: "unknown package: 31" — its `-p`
  flag cannot resolve platforms without a `platforms/` dir, so the
  plain-cargo route with explicit CC/CXX/AR/LINKER + BINDGEN sysroot
  args was used instead). Output verified: ELF 64-bit ARM aarch64,
  interpreter `/system/bin/linker64`, 4,996,368 B, staged at
  `manager/app/src/main/jniLibs/arm64-v8a/libksud.so` (included in the
  source snapshot; rebuild from `userspace/ksud` with the same env).
- Manager APK: NOT BUILT. The Gradle daemon-dispatch failure
  documented for this VM (`MessageIOException: Could not write
  '/127.0.0.1:<port>'` -> `IOException: Broken pipe`; the daemon starts,
  listens, and never receives the client's message) recurred on every
  attempt on 2026-10-31: Gradle 9.7.1 and 9.5.1, JDK 21 and JDK 25,
  `--no-daemon` and daemon mode, `-Xmx2g` and `-Xmx3g`, fresh daemon
  registry — 7 consecutive failures, zero dependency downloads, build
  never reached configuration. Loopback TCP itself was verified
  working with a plain socket test, so the failure is in the
  Gradle/JVM socket path in this environment, not the network. Logs:
  `~/workspace/cronchsu/build-logs/gradle*.log`. No proxy credentials
  were written anywhere; no cert/truststore work was done.
- Consequence: the rebrand code is COMPLETE and reviewed in-tree but
  UNCOMPILED. The Kotlin/Compose changes could not be type-checked on
  this VM. Known review-level risks, not hidden:
  (1) `FlowRow` opt-in — upstream uses `FlowRow` without an opt-in in
  `TemplateMiuix.kt`, so it is stable in this Compose version and the
  new usages should compile; (2) the Miuix `Icons.Rounded.Storage` /
  `MiuixIcons.Share` references in the new screens were taken from
  existing in-tree usages and must be confirmed by the first successful
  compile; (3) the Coop log console and credit rows added 2026-10-31
  (CronchLog, CronchLogConsole, About/DeviceInfo link rows) are likewise
  review-verified only — their Miuix `colorScheme` fields (`surface`,
  `onBackground`, `onSurfaceVariantSummary`, `primary`) and Material3
  `surfaceContainerLow` / `SwitchDefaults` usages were all verified
  against existing in-tree usages, but the first successful compile is
  still the real check. Retry the Gradle build from a healthy environment (or this
  VM on a day the daemon dispatch works — it succeeded once on
  2026-10-30 for a different project) with:
  `JAVA_HOME=<jdk21> ANDROID_HOME=<sdk> ./gradlew :app:assembleDebug
  -PKSU_NAME="Cronch SU"`. No APK exists yet; do not claim otherwise.

## Build

Manager build requires ksud binaries in `jniLibs` first (see upstream
AGENTS.md). This fork builds with:

```
./gradlew :app:assembleDebug -PKSU_NAME="Cronch SU"
```

after staging `libksud.so` (arm64-v8a) from `userspace/ksud`
(`cargo ndk -t arm64-v8a build --release`, NDK 29).

## GitHub Actions build (no PC needed)

`.github/workflows/build-cronch-su.yml` builds the debug APK on
GitHub: push this tree to a repo, open the Actions tab, run
"Build Cronch SU" (it also runs on push to `main`), then download
the `cronch-su-debug-apk` artifact. Debug-signed, so uninstall any
release-signed KernelSU manager first. The workflow itself has
never run yet — its first GitHub run is the real compile check.
