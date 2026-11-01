use adb_client::ADBDeviceExt;
use adb_client::tcp::ADBTcpDevice;
use anyhow::{Context, Result, bail};
use log::{error, info};
use prop_rs_android::resetprop::ResetProp;
use prop_rs_android::sys_prop;
use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use std::process::Command;

const fn resetprop() -> ResetProp {
    ResetProp {
        skip_svc: true,
        persistent: false,
        persist_only: false,
        verbose: false,
        show_context: false,
        rebuild: false,
    }
}

fn exec_shell_commands(commands: &[(&str, &[&str])], log_prefix: &str) -> Result<()> {
    for (cmd, args) in commands {
        info!("{log_prefix}: {cmd} {}", args.join(" "));
        let status = Command::new(cmd)
            .args(*args)
            .status()
            .with_context(|| format!("Failed to execute {cmd}"))?;
        if !status.success() {
            bail!("{cmd} {} exited with {status}", args.join(" "));
        }
    }
    Ok(())
}

fn enable_adb_root(port: u16) -> Result<()> {
    // We are in limited root by magica
    anyhow::ensure!(
        rustix::process::getuid().as_raw() == 0,
        "must be run as root"
    );

    sys_prop::init().context("Failed to initialize system property API")?;
    let rp = resetprop();

    let debuggable_context = sys_prop::get_context("ro.debuggable")
        .context("Failed to get context for ro.debuggable")?;
    info!("ro.debuggable context: {debuggable_context}");

    let adb_secure_context = sys_prop::get_context("ro.adb.secure")
        .context("Failed to get context for ro.adb.secure")?;
    info!("ro.adb.secure context: {adb_secure_context}");

    let props_serial = "/dev/__properties__/properties_serial";
    let debuggable_context = format!("/dev/__properties__/{debuggable_context}");
    let adb_secure_context = format!("/dev/__properties__/{adb_secure_context}");
    let port_str = port.to_string();

    // chmod property files to writable
    exec_shell_commands(
        &[
            ("chmod", &["0644", props_serial]),
            ("chmod", &["0644", &debuggable_context]),
            ("chmod", &["0644", &adb_secure_context]),
        ],
        "Executing",
    )?;

    // Set properties via internal API
    rp.set("ro.debuggable", "1")
        .context("Failed to set ro.debuggable")?;
    info!("Executing: resetprop -n ro.debuggable 1");
    rp.set("ro.adb.secure", "0")
        .context("Failed to set ro.adb.secure")?;
    info!("Executing: resetprop -n ro.adb.secure 0");

    // Restore permissions and restart adbd
    exec_shell_commands(
        &[
            ("chmod", &["0444", props_serial]),
            ("chmod", &["0444", &debuggable_context]),
            ("chmod", &["0444", &adb_secure_context]),
            ("setprop", &["service.adb.root", "1"]),
            ("setprop", &["service.adb.tcp.port", &port_str]),
            ("setprop", &["ctl.restart", "adbd"]),
        ],
        "Executing",
    )?;

    Ok(())
}

pub fn disable_adb_root() -> Result<()> {
    // We have full root now, no need to chmod
    sys_prop::init().context("Failed to initialize system property API")?;
    let rp = resetprop();

    info!("Restoring: resetprop -n ro.debuggable 0");
    rp.set("ro.debuggable", "0")
        .context("Failed to set ro.debuggable")?;

    info!("Restoring: resetprop -n ro.adb.secure 1");
    rp.set("ro.adb.secure", "1")
        .context("Failed to set ro.adb.secure")?;

    for prop in &[
        "service.adb.root",
        "service.adb.tcp.port",
        "ro.boot.selinux",
    ] {
        info!("Restoring: resetprop --delete {prop}");
        let _ = rp.delete(prop);
        if let Ok(ctx) = sys_prop::get_context(prop) {
            let _ = rp.rebuild(&ctx);
        }
    }

    exec_shell_commands(&[("setprop", &["ctl.restart", "adbd"])], "Restoring")?;

    Ok(())
}

/// True only for the staged late-load file the manager writes:
/// `<files>/lateload/<name>.ko` under one of the app data dirs for
/// `package_name`, with a flat filename from a strict alphabet, ending
/// in `.ko`, and no `..` segments. Used to keep a user-picked path from
/// ever reaching the adb-shell command string unsanitized.
fn is_staged_late_load_ko(package_name: &str, path: &str) -> bool {
    if package_name.is_empty()
        || !package_name
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || c == '.' || c == '_')
    {
        return false;
    }
    let prefixes = [
        format!("/data/data/{package_name}/files/lateload/"),
        format!("/data/user/0/{package_name}/files/lateload/"),
        format!("/data/user_de/0/{package_name}/files/lateload/"),
    ];
    let Some(name) = prefixes
        .iter()
        .find_map(|prefix| path.strip_prefix(prefix.as_str()))
    else {
        return false;
    };
    !name.is_empty()
        && name.ends_with(".ko")
        && !name.contains("..")
        && !name.contains('/')
        && name
            .chars()
            .all(|c| c.is_ascii_alphanumeric() || c == '.' || c == '-' || c == '_')
}

fn connect_to_device(port: u16) -> Result<ADBTcpDevice> {
    const MAX_RETRIES: u32 = 30;
    for attempt in 1..=MAX_RETRIES {
        info!("Waiting for adbd to restart... (attempt {attempt}/{MAX_RETRIES})");
        std::thread::sleep(std::time::Duration::from_secs(1));

        let addr = SocketAddr::new(IpAddr::V4(Ipv4Addr::LOCALHOST), port);
        info!("Connecting to ADB device at {addr}");
        match ADBTcpDevice::new(addr).context("Failed to create ADBTcpDevice") {
            Ok(device) => return Ok(device),
            Err(e) => {
                error!("Failed to connect to ADB device: {e:?}, retry after 1s");
            }
        }
    }
    bail!("Failed to connect to ADB device after {MAX_RETRIES} attempts")
}

pub fn run(
    port: u16,
    package_name: &String,
    allow_shell: bool,
    kmi: Option<String>,
    ko_path: Option<String>,
) -> Result<()> {
    enable_adb_root(port)?;

    let mut device = connect_to_device(port)?;

    let self_path = std::env::current_exe().context("Failed to get self exe path")?;

    // Execute late-load with --post-magica via adb shell.
    // The late-load process has full root + su domain and will:
    // 1. Load kernelsu.ko, enforce SELinux, run stage scripts
    // 2. Restore adb properties (disable adb root/tcp mode)
    // A user-picked KMI is forwarded so Magica loads the chosen embedded
    // chip instead of auto-detecting; anything outside the KMI alphabet is
    // refused because the value is interpolated into a shell command.
    let kmi_arg = match &kmi {
        Some(kmi) => {
            anyhow::ensure!(
                !kmi.is_empty()
                    && kmi
                        .chars()
                        .all(|c| c.is_ascii_alphanumeric() || c == '.' || c == '-' || c == '_'),
                "refusing unsafe KMI for late-load: {kmi}"
            );
            format!(" --kmi {kmi}")
        }
        None => String::new(),
    };
    let allow_shell_arg = if allow_shell { " --allow-shell" } else { "" };
    // A hand-picked .ko file is forwarded as --ko-path. The value is
    // interpolated into an adb-shell command string below, so it is
    // whitelisted hard: it must be the exact staged file the manager
    // wrote under this app's files dir, one flat filename, .ko only.
    // Anything else is refused before any shell runs.
    let ko_path_arg = match &ko_path {
        Some(path) => {
            anyhow::ensure!(
                is_staged_late_load_ko(package_name, path),
                "refusing unsafe --ko-path for late-load: {path}"
            );
            format!(" --ko-path {path}")
        }
        None => String::new(),
    };
    let cmd = format!(
        "{} late-load --post-magica --package-name {}{}{}{}",
        self_path.display(),
        package_name,
        allow_shell_arg,
        kmi_arg,
        ko_path_arg
    );
    info!("Executing '{cmd}' via adb shell...");
    let mut stdout = Vec::new();
    let mut stderr = Vec::new();
    if let Err(e) = device.shell_command(&cmd, Some(&mut stdout), Some(&mut stderr)) {
        info!("adb shell finished with error (may be expected): {e}");
    }
    if !stdout.is_empty() {
        info!("stdout: {}", String::from_utf8_lossy(&stdout));
    }
    if !stderr.is_empty() {
        info!("stderr: {}", String::from_utf8_lossy(&stderr));
    }

    Ok(())
}
