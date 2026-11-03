package me.weishu.kernelsu.ui.screen.activitylog

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.CronchDoritoOrange
import me.weishu.kernelsu.ui.theme.cronchLogContentColor
import me.weishu.kernelsu.ui.theme.cronchLogSurfaceColor
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun ActivityLogScreenMiuix(
    state: ActivityLogScreenState,
    actions: ActivityLogActions,
) {
    val isDark = isInDarkTheme()
    Scaffold(
        topBar = {
            TopAppBar(
                color = colorScheme.surface,
                title = stringResource(R.string.activity_log_title),
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
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
        ActivityLogContent(
            state = state,
            actions = actions,
            containerColor = cronchLogSurfaceColor(isDark),
            contentColor = cronchLogContentColor(isDark),
            mutedColor = cronchLogContentColor(isDark).copy(alpha = 0.7f),
            accentColor = CronchDoritoOrange,
            isDark = isDark,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 12.dp),
        )
    }
}
