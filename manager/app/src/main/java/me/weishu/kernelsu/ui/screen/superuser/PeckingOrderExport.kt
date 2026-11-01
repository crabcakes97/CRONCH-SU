package me.weishu.kernelsu.ui.screen.superuser

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Cronch SU: system file-picker export for generated text (Pecking Order
 * list, device info). The text is built lazily at write time from live
 * ViewModel state, written as UTF-8 through the SAF stream, and the result
 * (written vs failed) is reported back so the caller can show feedback.
 * Cancelling the picker reports nothing.
 */
@Composable
fun rememberExportTextLauncher(
    fileName: String,
    buildText: () -> String,
    onDone: (success: Boolean) -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { output ->
                        output.write(buildText().toByteArray(Charsets.UTF_8))
                    }
                }.isSuccess
            }
            onDone(ok)
        }
    }
    return { launcher.launch(fileName) }
}
