package me.weishu.kernelsu.ui.screen.home

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.R
import me.weishu.kernelsu.magica.MagicaService
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.component.choosekmidialog.ChooseKmiDialog
import me.weishu.kernelsu.ui.component.dialog.rememberLoadingDialog
import me.weishu.kernelsu.ui.component.lateloadchooser.LateLoadChooserDialog
import me.weishu.kernelsu.ui.navigation3.Navigator
import me.weishu.kernelsu.ui.navigation3.Route
import me.weishu.kernelsu.ui.screen.install.isKoFile
import me.weishu.kernelsu.ui.util.CronchLog
import me.weishu.kernelsu.ui.util.LateLoadChoice
import me.weishu.kernelsu.ui.util.stageLateLoadKo
import me.weishu.kernelsu.ui.util.writeLateLoadChoice
import me.weishu.kernelsu.ui.viewmodel.HomeViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun HomePager(
    navigator: Navigator,
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val viewModel = viewModel<HomeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    val loadingDialog = rememberLoadingDialog()
    val scope = rememberCoroutineScope()
    val latestIsCurrentPage by rememberUpdatedState(isCurrentPage)
    val initialResumeHandled = rememberSaveable { mutableStateOf(false) }

    var hasActivated by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(isCurrentPage) {
        if (isCurrentPage && !hasActivated) {
            hasActivated = true
            viewModel.refresh()
        }
    }

    LifecycleResumeEffect(Unit) {
        if (initialResumeHandled.value && latestIsCurrentPage) {
            viewModel.refresh()
        }
        initialResumeHandled.value = true
        onPauseOrDispose { }
    }

    var showLateLoadChooser by rememberSaveable { mutableStateOf(false) }
    var showChooseKmiForLateLoad by rememberSaveable { mutableStateOf(false) }

    // The choice is written before the service starts: the app-zygote preload
    // reads it and forwards it as ksud's --kmi/--ko-path flags. Auto means
    // auto-detect.
    fun launchLateLoad(choice: LateLoadChoice) {
        scope.launch(Dispatchers.IO) {
            writeLateLoadChoice(choice)
            withContext(Dispatchers.Main) {
                CronchLog.log("Root", "jailbreak requested (late-load)")
                loadingDialog.showLoading()
                context.startService(Intent(context, MagicaService::class.java))
            }
            // Manager will be force-stopped and restarted by late-load on success.
            // If that doesn't happen within timeout, jailbreak likely failed.
            delay(30_000.milliseconds)
            withContext(Dispatchers.Main) {
                loadingDialog.hide()
                CronchLog.log("Root", "jailbreak timed out after 30s (no restart — likely failed)")
                Toast.makeText(context, R.string.jailbreak_timeout, Toast.LENGTH_LONG).show()
            }
        }
    }

    // Hand-picked .ko for late-load: the content URI is not readable by the
    // root shell, so the pick is staged into the app's files dir first and
    // the staged path is what gets late-loaded. Invalid picks toast and
    // change nothing.
    val selectKoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                if (!isKoFile(context, uri)) {
                    Toast.makeText(context, R.string.install_only_support_ko_file, Toast.LENGTH_LONG).show()
                    return@let
                }
                scope.launch(Dispatchers.IO) {
                    val staged = stageLateLoadKo(context, uri)
                    withContext(Dispatchers.Main) {
                        if (staged == null) {
                            Toast.makeText(context, R.string.install_only_support_ko_file, Toast.LENGTH_LONG).show()
                        } else {
                            launchLateLoad(LateLoadChoice.KoFile(staged.first))
                        }
                    }
                }
            }
        }
    }

    val actions = HomeActions(
        onInstallClick = { navigator.push(Route.Install) },
        onOpenUrl = uriHandler::openUri,
        onJailbreakClick = {
            showLateLoadChooser = true
        },
        onOpenActivityLog = { navigator.push(Route.ActivityLog) },
        onOpenProviders = { navigator.push(Route.Providers) },
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> HomePagerMiuix(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )

        UiMode.Material -> HomePagerMaterial(
            state = uiState,
            actions = actions,
            bottomInnerPadding = bottomInnerPadding,
        )
    }

    LateLoadChooserDialog(
        show = showLateLoadChooser,
        onDismissRequest = { showLateLoadChooser = false },
        onConfirm = { choice ->
            showLateLoadChooser = false
            launchLateLoad(choice)
        },
        onPickBloodline = {
            showLateLoadChooser = false
            showChooseKmiForLateLoad = true
        },
        onPickKoFile = {
            showLateLoadChooser = false
            selectKoLauncher.launch(Intent(Intent.ACTION_GET_CONTENT).apply { type = "application/octet-stream" })
        },
    )
    ChooseKmiDialog(
        show = showChooseKmiForLateLoad,
        onDismissRequest = { showChooseKmiForLateLoad = false },
        onSelected = { kmi ->
            showChooseKmiForLateLoad = false
            kmi?.let { launchLateLoad(LateLoadChoice.Kmi(it)) }
        },
    )
}
