package me.weishu.kernelsu.ui.component.lateloadchooser

import androidx.compose.runtime.Composable
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.util.LateLoadChoice

/**
 * The CronchBreak chip picker: what the Magica late-load should peck into the
 * kernel. [onConfirm] gets the chosen [LateLoadChoice]: auto-detect (the
 * Council sniffs the kernel and picks the matching embedded chip), a KMI
 * (the chosen embedded chip), or a hand-picked staged .ko file.
 * [onPickBloodline] opens the bloodline list; the KMI picked there confirms
 * the late-load. [onPickKoFile] opens the .ko file picker; the staged file
 * confirms the late-load.
 */
@Composable
fun LateLoadChooserDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (LateLoadChoice) -> Unit,
    onPickBloodline: () -> Unit,
    onPickKoFile: () -> Unit,
) {
    when (LocalUiMode.current) {
        UiMode.Miuix -> LateLoadChooserDialogMiuix(show, onDismissRequest, onConfirm, onPickBloodline, onPickKoFile)
        UiMode.Material -> LateLoadChooserDialogMaterial(show, onDismissRequest, onConfirm, onPickBloodline, onPickKoFile)
    }
}

/** Human size for a staged .ko: "512 KB" / "1.2 MB". True bytes, no lore math. */
internal fun formatKoSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.0f KB".format(kb)
    return "%.1f MB".format(kb / 1024.0)
}
