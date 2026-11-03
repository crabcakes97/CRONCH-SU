package me.weishu.kernelsu.ui.screen.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.material.SegmentedColumn
import me.weishu.kernelsu.ui.component.material.SegmentedListItem
import me.weishu.kernelsu.ui.theme.CronchDoritoOrange
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon as MiuixIcon
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.textStyles

/**
 * Dashboard summary card for the Five Perches (see ProvidersScreen).
 * Shows the provider actually serving root right now — derived from the
 * same live probes as the Providers screen — and opens it on tap.
 */
@Composable
fun ProviderSummaryCardMaterial(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val summary = state.activeProvider
    SegmentedColumn(modifier = modifier.fillMaxWidth()) {
        item {
            SegmentedListItem(
                onClick = actions.onOpenProviders,
                headlineContent = { Text(stringResource(id = R.string.providers_summary_title)) },
                supportingContent = {
                    Text(
                        summary.detail
                            ?: stringResource(id = R.string.providers_summary_none)
                    )
                },
                leadingContent = {
                    Icon(
                        Icons.Filled.Security,
                        stringResource(id = R.string.providers_summary_title),
                    )
                },
                trailingContent = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        summary.headline?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            null,
                        )
                    }
                },
            )
        }
    }
}

@Composable
fun ProviderSummaryCardMiuix(
    state: HomeUiState,
    actions: HomeActions,
    modifier: Modifier = Modifier,
) {
    val summary = state.activeProvider
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = actions.onOpenProviders,
        showIndication = true,
        insideMargin = PaddingValues(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            MiuixIcon(
                imageVector = Icons.Rounded.Security,
                contentDescription = stringResource(id = R.string.providers_summary_title),
                modifier = Modifier.padding(end = 12.dp).size(24.dp),
                tint = CronchDoritoOrange,
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                MiuixText(
                    text = stringResource(id = R.string.providers_summary_title),
                    fontSize = textStyles.headline1.fontSize,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                )
                MiuixText(
                    text = summary.detail
                        ?: stringResource(id = R.string.providers_summary_none),
                    fontSize = textStyles.body2.fontSize,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                summary.headline?.let {
                    MiuixText(
                        text = it,
                        fontSize = textStyles.body2.fontSize,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.primary,
                    )
                }
                MiuixText(
                    text = stringResource(id = R.string.providers_summary_open),
                    fontSize = textStyles.body2.fontSize,
                    color = colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
