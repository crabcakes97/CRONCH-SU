package me.weishu.kernelsu.ui.screen.about

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.dropUnlessResumed
import me.weishu.kernelsu.BuildConfig
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.LocalUiMode
import me.weishu.kernelsu.ui.UiMode
import me.weishu.kernelsu.ui.navigation3.LocalNavigator

@Composable
fun AboutScreen() {
    val navigator = LocalNavigator.current
    val uriHandler = LocalUriHandler.current
    val htmlString = stringResource(
        id = R.string.about_source_code,
        "<b><a href=\"https://github.com/tiann/KernelSU\">GitHub</a></b>",
        "<b><a href=\"https://t.me/KernelSU\">Telegram</a></b>"
    )
    val state = AboutUiState(
        title = stringResource(R.string.about),
        appName = stringResource(R.string.app_name),
        versionName = BuildConfig.VERSION_NAME,
        // Existing upstream links stay first and untouched; the fork and
        // upstream credits append after them. Every row is tappable and
        // opens its URL in the browser via the same uri handler — no
        // plain-text URLs anywhere in the credits.
        links = extractLinks(htmlString) + listOf(
            LinkInfo(
                fullText = stringResource(R.string.cronch_credit_fork),
                url = "https://github.com/crabcakes97",
            ),
            LinkInfo(
                fullText = stringResource(R.string.cronch_credit_upstream),
                url = "https://github.com/tiann/KernelSU",
            ),
            LinkInfo(
                fullText = stringResource(R.string.cronch_credit_next),
                url = "https://github.com/KernelSU-Next/KernelSU-Next",
            ),
        ),
    )
    val actions = AboutScreenActions(
        onBack = dropUnlessResumed { navigator.pop() },
        onOpenLink = uriHandler::openUri,
    )

    when (LocalUiMode.current) {
        UiMode.Miuix -> AboutScreenMiuix(state, actions)
        UiMode.Material -> AboutScreenMaterial(state, actions)
    }
}
