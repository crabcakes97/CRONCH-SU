# CRONCH SU 🐔🌮

> CRONCH. PECKS COOL RANCH DORITO 🐔🌮

## What is this

The Coop is open, and the chickens are in charge now.

CRONCH SU is the KernelSU manager, rebuilt the way the Poultry Council
would want it: absurd on the surface, serious underneath. The Poultry
Council governs all snack distribution. The Four Pillars watch over
every screen. Moai 🗿 brings absolute indifference. The Distorted One 🌀
brings bewilderment. Slay 💅 brings fabulous execution. Hello Kitty 🎀
brings sacred cuteness. The Crustacean Division 🦀 handles the gumbo.

Everything in here actually works. Every button does what it says.
The nonsense is the paint job, not the engine.

The Council eats first.

## The Coop

Home base. A rainbow log console sits at the top of the screen and
narrates everything the app is doing, in real time, in rainbow, with
real timestamps. What it did, what failed, what changed. No made-up
lines, no theater. Clear it or copy it from the header. The rainbow
can be switched off, and the switch remembers.

## Pecking Order

Who gets the good snacks, and who gets pecked away. Per-app rules,
saved properly, with honest failure messages when a save does not
stick. The list can be exported, because the Council believes in
paperwork.

## Flavor Stash

Modules live here. Filter them, switch them on and off, flash them,
undo an uninstall when you change your mind. The stash shows real
state only. If something is not installed, it says so.

## Council Settings

Twenty themes: ten flavors from the archive, each in Day and After
Dark. Original, Alpine Ranch, Louisiana Crab Gumbo, Rotisserie
Chicken, Nuclear Ranch, Nacho Rebellion, Nacho Ranch, Family Reserve,
Emergency Stash, Mystery CRONCH. Plus a custom color picker with
presets and a live preview, because the Council has opinions about
shades of orange.

Device Info shows live, real numbers only: memory, CPU, uptime,
storage, battery. If a value cannot be read, it says Unavailable.
It never invents a number.

Next goodies, borrowed from KernelSU-Next and folded into this one
unified build: hook-mode and version-tag probes, the manager appid
probe, Zygisk status, and AVC-spoof status. Base behavior is
preserved. Nothing that works gets broken to make room.

The six Official CRONCH Employee Rituals are observed at all times:
the Sacred Peck, the Moai Vigil, the Ranch Offering, the Cronch
Anthem, the Slay Ceremony, and the Final CRONCH. There is no HR
department. Only chickens.

## Build it

No PC needed. Push this tree to GitHub and the included workflow
builds the app in the cloud:

1. Push to `main`.
2. Open the Actions tab, watch **Build Cronch SU** run.
3. Download the `cronch-su-debug-apk` artifact.

Or build it yourself, from the `manager/` folder, after staging the
`libksud.so` binary (see `CRONCHSU.md` for the full notes):

```
./gradlew :app:assembleDebug -PKSU_NAME="Cronch SU"
```

Install notes, plainly: uninstall any release-signed KernelSU manager
first, because a debug-signed build cannot update over it. The package
id stays `me.weishu.kernelsu` on purpose, the kernel side expects it.
A stock kernel will not crown a debug-signed manager. Behavior against
a KernelSU-Next LKM setup is untested. No APK is checked into this
repo; the workflow builds it fresh every time.

## The serious bit (read this last)

root makes phone go brrrrr.

Thank you to the KernelSU devs, led by tiann
(https://github.com/tiann/KernelSU), and to the KernelSU-Next devs
(https://github.com/KernelSU-Next/KernelSU-Next), whose work this fork
is built on. And a nod to Chainfire, the OG. This Coop was pecked
into existence by [crabcakes97](https://github.com/crabcakes97).

Licensed under GPL-3.0. See `LICENSE`. The lore is fictional.
The work is real.
