package me.weishu.kernelsu.ui.util

import android.text.format.DateFormat
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * Cronch SU verbose log: a bounded, in-memory ring buffer of everything the
 * app is doing, rendered by the Coop log console at the top of the home
 * screen. Every entry carries a real timestamp and a real message from a
 * real call site — nothing here is simulated, placeholder, or invented.
 *
 * Logging is deliberately failure-proof: [log] never throws, never blocks,
 * and never alters the operation being logged. Calls from any thread are
 * safe. The buffer is capped at [MAX_LINES] so the log can never grow
 * memory without limit.
 */
object CronchLog {

    /** Maximum lines kept; older lines drop off the top as new ones arrive. */
    const val MAX_LINES = 500

    data class Entry(
        val timeMillis: Long,
        val seq: Long,
        val tag: String,
        val message: String,
    )

    private val seq = AtomicLong(0L)

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    private val launchLogged = AtomicBoolean(false)

    // Rainbow preference, backed by the same SharedPreferences file the rest
    // of settings use. Loaded lazily so a prefs failure can never break
    // logging; the toggle still works in-memory for the session.
    private val prefsRepo by lazy { runCatching { SettingsRepositoryImpl() }.getOrNull() }
    private val rainbowLoaded = AtomicBoolean(false)
    private val _rainbowEnabled = MutableStateFlow(true)
    val rainbowEnabled: StateFlow<Boolean> = _rainbowEnabled.asStateFlow()

    fun ensurePrefsLoaded() {
        if (!rainbowLoaded.compareAndSet(false, true)) return
        runCatching {
            prefsRepo?.let { _rainbowEnabled.value = it.rainbowLog }
        }
    }

    fun setRainbowEnabled(enabled: Boolean) {
        _rainbowEnabled.value = enabled
        runCatching {
            prefsRepo?.rainbowLog = enabled
        }
    }

    fun log(tag: String, message: String) {
        try {
            val entry = Entry(
                timeMillis = System.currentTimeMillis(),
                seq = seq.getAndIncrement(),
                tag = tag,
                message = message,
            )
            _entries.update { (it + entry).takeLast(MAX_LINES) }
        } catch (_: Exception) {
            // Logging must never crash or alter the logged operation.
        }
    }

    /** One honest line when the app process starts; guarded so rotations and
     *  re-creations never duplicate it. */
    fun logAppLaunch() {
        if (launchLogged.compareAndSet(false, true)) {
            log("Coop", "Cronch SU launched — Coop log live. CRONCH. PECKS COOL RANCH DORITO")
        }
    }

    /** Empties the buffer, then leaves exactly one honest line saying so. */
    fun clear() {
        try {
            _entries.value = listOf(
                Entry(
                    timeMillis = System.currentTimeMillis(),
                    seq = seq.getAndIncrement(),
                    tag = "Coop",
                    message = "Log cleared",
                )
            )
        } catch (_: Exception) {
            // Clearing must never crash either.
        }
    }

    /** Plain-text rendering of the current buffer, for the Copy action. */
    fun formatForCopy(): String = buildString {
        for (entry in _entries.value) {
            appendLine("[${formatTime(entry.timeMillis)}] [${entry.tag}] ${entry.message}")
        }
    }

    fun formatTime(timeMillis: Long): String =
        DateFormat.format("HH:mm:ss", timeMillis).toString()

    /**
     * Rainbow color for one log line. The hue rotates by the golden angle
     * per line so neighbors always differ clearly. Lightness is chosen for
     * readability on both themes: bright and saturated on dark surfaces,
     * dark and saturated on light surfaces. The log background itself is
     * always a plain theme surface — never rainbow-on-rainbow.
     */
    fun rainbowColor(entry: Entry, isDark: Boolean): Color {
        val hue = (entry.seq * 137L % 360L).toFloat()
        return if (isDark) {
            Color.hsv(hue, 0.62f, 0.95f)
        } else {
            Color.hsv(hue, 0.85f, 0.42f)
        }
    }
}
