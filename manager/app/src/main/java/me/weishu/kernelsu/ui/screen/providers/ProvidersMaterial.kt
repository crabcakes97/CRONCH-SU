package me.weishu.kernelsu.ui.screen.providers

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.material.ExpressiveScaffold
import me.weishu.kernelsu.ui.component.material.TopBarBackButton
import me.weishu.kernelsu.ui.component.statustag.StatusTag
import me.weishu.kernelsu.ui.provider.CapabilityState
import me.weishu.kernelsu.ui.provider.DetectionState
import me.weishu.kernelsu.ui.provider.ProviderInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProvidersScreenMaterial(
    state: ProvidersScreenState,
    actions: ProvidersActions,
) {
    ExpressiveScaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.providers_title)) },
                navigationIcon = { TopBarBackButton(onClick = actions.onBack) },
                actions = {
                    IconButton(onClick = actions.onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.providers_refresh),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing.only(
            WindowInsetsSides.Top + WindowInsetsSides.Horizontal
        ),
    ) { innerPadding ->
        if (state.isLoading && state.providers.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
            return@ExpressiveScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = stringResource(R.string.providers_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(state.providers, key = { it.id }) { provider ->
                ProviderCardMaterial(provider)
            }
            item {
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun ProviderCardMaterial(provider: ProviderInfo) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceContainerLow),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = providerDisplayName(provider.id),
                    style = MaterialTheme.typography.titleMedium,
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
                        colorScheme.surfaceVariant,
                        colorScheme.onSurfaceVariant,
                    )

                    DetectionState.DETECTION_UNAVAILABLE -> Triple(
                        R.string.provider_status_unavailable,
                        colorScheme.tertiary,
                        colorScheme.onTertiary,
                    )
                }
                StatusTag(label = stringResource(labelRes), backgroundColor = bg, contentColor = fg)
            }
            provider.evidence?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = providerNote(provider.id),
                style = MaterialTheme.typography.bodyMedium,
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
                            CapabilityState.UNSUPPORTED -> "✗" to colorScheme.onSurfaceVariant
                        }
                        Text(text = glyph, color = glyphColor, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = providerCapabilityLabel(capability.id),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        val stateRes = when (capability.state) {
                            CapabilityState.INTEGRATED -> R.string.provider_cap_integrated
                            CapabilityState.UPSTREAM_ONLY -> R.string.provider_cap_upstream_only
                            CapabilityState.UNSUPPORTED -> R.string.provider_cap_unsupported
                        }
                        Text(
                            text = stringResource(stateRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}
