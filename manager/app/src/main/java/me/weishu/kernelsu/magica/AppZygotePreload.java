package me.weishu.kernelsu.magica;

import android.app.ZygotePreload;
import android.content.pm.ApplicationInfo;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.File;

public class AppZygotePreload implements ZygotePreload {
    public static final String TAG = "KernelSUMagica";

    private static native void forkDontCareAndExecKsud(String ksudPath, String packageName, String kmi, String koPath);

    // The KMI picked in the late-load chooser, written by the manager to
    // <dataDir>/files/magica-kmi (see KsuCli.writeLateLoadKmi). Missing or
    // blank means auto-detect, exactly like before. The preload runs before
    // the app is unlocked at locked-boot time, so an unreadable file also
    // falls back to auto-detect.
    private static String readLateLoadKmi(ApplicationInfo appInfo) {
        try {
            java.io.File file = new java.io.File(appInfo.dataDir, "files/magica-kmi");
            StringBuilder sb = new StringBuilder();
            try (java.io.BufferedReader reader =
                         new java.io.BufferedReader(new java.io.FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            }
            String kmi = sb.toString().trim();
            return kmi.isEmpty() ? null : kmi;
        } catch (Throwable t) {
            return null;
        }
    }

    @Override
    public void doPreload(@NonNull ApplicationInfo appInfo) {
        File f = new File(appInfo.nativeLibraryDir, "libksud.so");
        try {
            System.loadLibrary("kernelsu");
            Log.d(TAG, "executing magica ...");
            forkDontCareAndExecKsud(f.getAbsolutePath(), appInfo.packageName, readLateLoadKmi(appInfo), readLateLoadKoPath(appInfo));
        } catch (Throwable t) {
            Log.e(TAG, "failed to late load", t);
        }
    }

    // The hand-picked .ko staged by the manager in
    // <dataDir>/files/lateload/<name>.ko, recorded in <dataDir>/files/magica-ko
    // (see KsuCli.writeLateLoadChoice). Only the exact staged shape is
    // accepted: anything else falls back to the KMI choice or auto-detect.
    // ksud re-validates the same whitelist before the value ever reaches
    // a shell command; this reader just refuses to forward junk.
    private static String readLateLoadKoPath(ApplicationInfo appInfo) {
        try {
            java.io.File file = new java.io.File(appInfo.dataDir, "files/magica-ko");
            String path;
            try (java.io.BufferedReader reader =
                         new java.io.BufferedReader(new java.io.FileReader(file))) {
                path = reader.readLine();
            }
            if (path == null) {
                return null;
            }
            path = path.trim();
            String prefix = appInfo.dataDir + "/files/lateload/";
            if (!path.startsWith(prefix)) {
                return null;
            }
            String name = path.substring(prefix.length());
            if (name.isEmpty() || !name.endsWith(".ko") || name.contains("..")
                    || name.contains("/") || !name.matches("[A-Za-z0-9._-]+")) {
                return null;
            }
            return path;
        } catch (Throwable t) {
            return null;
        }
    }
}
