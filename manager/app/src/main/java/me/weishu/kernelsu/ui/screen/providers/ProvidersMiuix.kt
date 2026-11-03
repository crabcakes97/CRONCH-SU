package me.weishu.kernelsu.ui.screen.providers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.statustag.StatusTag
import me.weishu.kernelsu.ui.provider.CapabilityState
import me.weishu.kernelsu.ui.provider.DetectionState
import me.weishu.kernelsu.ui.provider.ProviderInfo
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
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
fun ProvidersScreenMiuix(
    state: ProvidersScreenState,
    actions: ProvidersActions,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                color = colorScheme.surface,
                title = stringResource(R.string.providers_title),
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            imageVector = MiuixIcons.Back,
                            contentDescription = null,
                            tint = colorScheme.onBackground,
                        )
                    }
                },
                actions = {
                    TextButton(
                        text = stringResource(R.string.providers_refresh),
                        onClick = actions.onRefresh,
                        colors = ButtonDefaults.textButtonColorsPrimary(),
                    )
                },
            )
        },
        popupHost = { },
    ) { innerPadding ->
        if (state.isLoading && state.providers.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding() + 6.dp,
                bottom = 12.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.providers_subtitle),
                    fontSize = 14.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
            items(state.providers, key = { it.id }) { provider ->
                ProviderCardMiuix(provider)
            }
            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProviderCardMiuix(provider: ProviderInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        insideMargin = PaddingValues(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = providerDisplayName(provider.id),
                    fontSize = 17.sp,
                    fontWeight = FontWeight(550),
                    color = colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                val (labelRes, bg, fg) = when (provider.detection) {
                    DetectionState.DETECTED -> Triple(
                        R.string.provider_status_detected,
                        colorScheme.primary,
                        colorScheme.onPrimary,
                    )

                    DetectionState.NOT_DETECTED -> Triple(
                        R.string.provider_status_not_detected,
                        colorScheme.secondaryContainer,
                        colorScheme.onSecondaryContainer,
                    )

                    DetectionState.DETECTION_UNAVAILABLE -> Triple(
                        R.string.provider_status_unavailable,
                        colorScheme.tertiaryContainer,
                        colorScheme.onTertiaryContainer,
                    )
                }
                StatusTag(label = stringResource(labelRes), backgroundColor = bg, contentColor = fg)
            }
            provider.evidence?.let {
                Text(
                    text = it,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
            Text(
                text = providerNote(provider.id),
                fontSize = 14.sp,
                color = colorScheme.onSurface,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                provider.capabilities.forEach { capability ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val (glyph, glyphColor) = when (capability.state) {
                            CapabilityState.INTEGRATED -> "✓" to colorScheme.primary
                            CapabilityState.UPSTREAM_ONLY -> "◌" to colorScheme.tertiary
                            CapabilityState.UNSUPPORTED -> "✗" to colorScheme.onSurfaceVariantSummary
                        }
                        Text(text = glyph, color = glyphColor, fontSize = 14.sp)
                        Text(
                            text = providerCapabilityLabel(capability.id),
                            fontSize = 14.sp,
                            color = colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        val stateRes = when (capability.state) {
                            CapabilityState.INTEGRATED -> R.string.provider_cap_integrated
                            CapabilityState.UPSTREAM_ONLY -> R.string.provider_cap_upstream_only
                            CapabilityState.UNSUPPORTED -> R.string.provider_cap_unsupported
                        }
                        Text(
                            text = stringResource(stateRes),
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }
}
