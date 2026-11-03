package me.weishu.kernelsu.ui.screen.activitylog

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.TopBarBackButton
import me.weishu.kernelsu.ui.theme.CronchDoritoOrange
import me.weishu.kernelsu.ui.theme.cronchLogContentColor
import me.weishu.kernelsu.ui.theme.cronchLogSurfaceColor
import me.weishu.kernelsu.ui.theme.isInDarkTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLogScreenMaterial(
    state: ActivityLogScreenState,
    actions: ActivityLogActions,
) {
    val isDark = isInDarkTheme()
    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.activity_log_title)) },
                navigationIcon = { TopBarBackButton(onClick = actions.onBack) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}
