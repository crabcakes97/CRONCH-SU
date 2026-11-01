package me.weishu.kernelsu.ui.screen.deviceinfo

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.dropUnlessResumed
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.navigation3.LocalNavigator
import me.weishu.kernelsu.ui.screen.superuser.rememberExportTextLauncher
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun DeviceInfoMiuix() {
    val viewModel = viewModel<DeviceInfoViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = System.currentTimeMillis()
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            viewModel.refresh()
        }
    }
    val copied = stringResource(R.string.device_info_copied)
    val exportSaved = stringResource(R.string.log_saved)
    val exportFailed = stringResource(R.string.export_su_list_failed)
    val launchExport = rememberExportTextLauncher(
        fileName = "cronch-su-device-info.txt",
        buildText = viewModel::buildDeviceInfoExport,
    ) { ok ->
        Toast.makeText(
            context,
            if (ok) exportSaved else exportFailed,
            Toast.LENGTH_SHORT,
        ).show()
    }

    val sections = deviceInfoSections(state)

    Scaffold(
        topBar = {
            TopAppBar(
                color = colorScheme.surface,
                title = stringResource(R.string.device_info_title),
                navigationIcon = {
                    IconButton(onClick = dropUnlessResumed { navigator.pop() }) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = null,
                            tint = colorScheme.onBackground,
                        )
                    }
                },
            )
        },
        popupHost = { },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                top = 12.dp,
                start = 12.dp,
                end = 12.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.device_info_subtitle),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            items(sections) { section ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(text = section.title)
                        section.rows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(text = row.label, color = colorScheme.onSurfaceVariantActions)
                                Text(text = row.value)
                            }
                        }
                        if (section.title == stringResource(R.string.device_info_ram)) {
                            state.ramUsageFraction?.let { fraction ->
                                CronchMiuixProgressBar(fraction)
                            }
                        }
                        if (section.title == stringResource(R.string.device_info_storage)) {
                            CronchMiuixProgressBar(state.storageUsageFraction)
                        }
                        section.link?.let { link ->
                            TextButton(
                                text = link.label,
                                onClick = { uriHandler.openUri(link.url) },
                            )
                        }
                    }
                }
            }
            item {
                val ageSeconds = ((now - state.lastUpdatedAt) / 1000).coerceAtLeast(0)
                Text(
                    text = stringResource(R.string.device_info_updated_ago, ageSeconds.toInt()),
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        text = stringResource(R.string.device_info_refresh),
                        onClick = { viewModel.refresh() },
                    )
                    TextButton(
                        text = stringResource(R.string.device_info_copy),
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(
                                ClipData.newPlainText(
                                    "Cronch SU device info",
                                    viewModel.buildDeviceInfoExport(),
                                ),
                            )
                            Toast.makeText(context, copied, Toast.LENGTH_SHORT).show()
                        },
                    )
                    TextButton(
                        text = stringResource(R.string.device_info_export),
                        onClick = launchExport,
                    )
                }
            }
        }
    }
}

/** Determinate progress bar from primitives (Miuix has no determinate linear
 *  bar): track in surfaceVariant, fill in primary. */
@Composable
private fun CronchMiuixProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(colorScheme.primary),
        )
    }
}
