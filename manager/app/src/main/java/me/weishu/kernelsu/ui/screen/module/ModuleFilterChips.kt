package me.weishu.kernelsu.ui.screen.module

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import top.yukonga.miuix.kmp.basic.Text as MiuixText
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme as miuixColorScheme

/**
 * Enabled/disabled filter chips for the Flavor Stash (module list), in both
 * UI modes. The filter itself lives in ModuleViewModel and applies to both
 * the plain list and search results; these chips only select it.
 */
@Composable
fun ModuleFilterChipsMaterial(
    statusFilter: ModuleStatusFilter,
    onSetStatusFilter: (ModuleStatusFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(R.string.module_filter_label),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 10.dp),
        )
        FilterChip(
            selected = statusFilter == ModuleStatusFilter.ALL,
            onClick = { onSetStatusFilter(ModuleStatusFilter.ALL) },
            label = { Text(stringResource(R.string.module_filter_all)) },
        )
        FilterChip(
            selected = statusFilter == ModuleStatusFilter.ENABLED,
            onClick = { onSetStatusFilter(ModuleStatusFilter.ENABLED) },
            label = { Text(stringResource(R.string.module_filter_enabled)) },
        )
        FilterChip(
            selected = statusFilter == ModuleStatusFilter.DISABLED,
            onClick = { onSetStatusFilter(ModuleStatusFilter.DISABLED) },
            label = { Text(stringResource(R.string.module_filter_disabled)) },
        )
    }
}

@Composable
fun ModuleFilterChipsMiuix(
    statusFilter: ModuleStatusFilter,
    onSetStatusFilter: (ModuleStatusFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MiuixText(
            text = stringResource(R.string.module_filter_label),
            modifier = Modifier.padding(top = 8.dp),
        )
        listOf(
            ModuleStatusFilter.ALL to R.string.module_filter_all,
            ModuleStatusFilter.ENABLED to R.string.module_filter_enabled,
            ModuleStatusFilter.DISABLED to R.string.module_filter_disabled,
        ).forEach { (filter, labelRes) ->
            val selected = statusFilter == filter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) miuixColorScheme.primary else miuixColorScheme.outline,
                        shape = RoundedCornerShape(20.dp),
                    )
                    .clickable { onSetStatusFilter(filter) }
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                MiuixText(text = stringResource(labelRes))
            }
        }
    }
}
