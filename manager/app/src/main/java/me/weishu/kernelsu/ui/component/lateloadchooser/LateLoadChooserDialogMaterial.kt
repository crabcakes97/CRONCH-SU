package me.weishu.kernelsu.ui.component.lateloadchooser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.material.SegmentedColumn
import me.weishu.kernelsu.ui.component.material.SegmentedRadioItem
import me.weishu.kernelsu.ui.theme.CronchFlavorStripColors
import me.weishu.kernelsu.ui.util.LateLoadChoice
import me.weishu.kernelsu.ui.util.getCurrentKmi
import me.weishu.kernelsu.ui.util.readLateLoadChoice
import java.io.File

@Composable
fun LateLoadChooserDialogMaterial(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (LateLoadChoice) -> Unit,
    onPickBloodline: () -> Unit,
    onPickKoFile: () -> Unit,
) {
    if (!show) return

    val savedChoice by produceState<LateLoadChoice>(initialValue = LateLoadChoice.Auto) {
        value = readLateLoadChoice()
    }
    val currentKmi by produceState(initialValue = "") {
        value = getCurrentKmi()
    }
    var selection by remember(savedChoice) { mutableStateOf(savedChoice) }
    val savedKmi = (savedChoice as? LateLoadChoice.Kmi)?.kmi ?: ""
    val stagedKo = savedChoice as? LateLoadChoice.KoFile

    fun reset() {
        selection = savedChoice
    }

    AlertDialog(
        onDismissRequest = {
            onDismissRequest()
            reset()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(selection)
                    onDismissRequest()
                }
            ) {
                Text(stringResource(id = R.string.confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onDismissRequest()
                reset()
            }) {
                Text(stringResource(id = android.R.string.cancel))
            }
        },
        title = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                text = stringResource(R.string.lateload_chooser_title),
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column {
                SegmentedColumn(
                    content = listOf(
                        {
                            SegmentedRadioItem(
                                title = stringResource(R.string.lateload_option_auto),
                                summary = if (currentKmi.isBlank()) {
                                    stringResource(R.string.lateload_option_auto_summary)
                                } else {
                                    stringResource(R.string.current_kmi, currentKmi)
                                },
                                selected = selection is LateLoadChoice.Auto,
                                onClick = { selection = LateLoadChoice.Auto }
                            )
                        },
                        {
                            SegmentedRadioItem(
                                title = stringResource(R.string.lateload_option_pick),
                                summary = if (savedKmi.isBlank()) {
                                    stringResource(R.string.lateload_option_pick_summary)
                                } else {
                                    stringResource(R.string.current_kmi, savedKmi)
                                },
                                selected = selection is LateLoadChoice.Kmi,
                                onClick = { onPickBloodline() }
                            )
                        },
                        {
                            SegmentedRadioItem(
                                title = stringResource(R.string.lateload_option_file),
                                summary = if (stagedKo != null) {
                                    val staged = File(stagedKo.absolutePath)
                                    stringResource(
                                        R.string.lateload_option_file_staged,
                                        staged.name,
                                        formatKoSize(staged.length())
                                    )
                                } else {
                                    stringResource(R.string.lateload_option_file_summary)
                                },
                                selected = selection is LateLoadChoice.KoFile,
                                onClick = {
                                    if (stagedKo != null) {
                                        selection = stagedKo
                                    } else {
                                        onPickKoFile()
                                    }
                                }
                            )
                        }
                    )
                )
                Row(modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)) {
                    CronchFlavorStripColors.forEach { flavorColor ->
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .background(flavorColor)
                        )
                    }
                }
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    text = stringResource(R.string.lateload_ai_council_line),
                    textAlign = TextAlign.Center
                )
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    text = stringResource(R.string.lateload_chooser_footer),
                    textAlign = TextAlign.Center
                )
            }
        }
    )
}
