package me.weishu.kernelsu.ui.component.lateloadchooser

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.CronchFlavorStripColors
import me.weishu.kernelsu.ui.util.LateLoadChoice
import me.weishu.kernelsu.ui.util.getCurrentKmi
import me.weishu.kernelsu.ui.util.readLateLoadChoice
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.CheckboxLocation
import top.yukonga.miuix.kmp.preference.CheckboxPreference
import java.io.File

@Composable
fun LateLoadChooserDialogMiuix(
    show: Boolean,
    onDismissRequest: () -> Unit,
    onConfirm: (LateLoadChoice) -> Unit,
    onPickBloodline: () -> Unit,
    onPickKoFile: () -> Unit,
) {
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

    OverlayDialog(
        show = show,
        title = stringResource(R.string.lateload_chooser_title),
        summary = stringResource(R.string.lateload_chooser_footer),
        onDismissRequest = {
            onDismissRequest()
            reset()
        },
        insideMargin = DpSize(0.dp, 24.dp),
        content = {
            Column(modifier = Modifier.heightIn(max = 500.dp)) {
                Column {
                    CheckboxPreference(
                        title = stringResource(R.string.lateload_option_auto),
                        summary = if (currentKmi.isBlank()) {
                            stringResource(R.string.lateload_option_auto_summary)
                        } else {
                            stringResource(R.string.current_kmi, currentKmi)
                        },
                        insideMargin = PaddingValues(horizontal = 30.dp, vertical = 16.dp),
                        checkboxLocation = CheckboxLocation.End,
                        checked = selection is LateLoadChoice.Auto,
                        holdDownState = selection is LateLoadChoice.Auto,
                        onCheckedChange = { _ ->
                            selection = LateLoadChoice.Auto
                        }
                    )
                    CheckboxPreference(
                        title = stringResource(R.string.lateload_option_pick),
                        summary = if (savedKmi.isBlank()) {
                            stringResource(R.string.lateload_option_pick_summary)
                        } else {
                            stringResource(R.string.current_kmi, savedKmi)
                        },
                        insideMargin = PaddingValues(horizontal = 30.dp, vertical = 16.dp),
                        checkboxLocation = CheckboxLocation.End,
                        checked = selection is LateLoadChoice.Kmi,
                        holdDownState = selection is LateLoadChoice.Kmi,
                        onCheckedChange = { _ ->
                            onPickBloodline()
                        }
                    )
                    CheckboxPreference(
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
                        insideMargin = PaddingValues(horizontal = 30.dp, vertical = 16.dp),
                        checkboxLocation = CheckboxLocation.End,
                        checked = selection is LateLoadChoice.KoFile,
                        holdDownState = selection is LateLoadChoice.KoFile,
                        onCheckedChange = { _ ->
                            if (stagedKo != null) {
                                selection = stagedKo
                            } else {
                                onPickKoFile()
                            }
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    CronchFlavorStripColors.forEach { flavorColor ->
                        Spacer(
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .background(flavorColor)
                        )
                    }
                }
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(R.string.lateload_ai_council_line),
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            onDismissRequest()
                            reset()
                        },
                        text = stringResource(android.R.string.cancel),
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(20.dp))
                    TextButton(
                        onClick = {
                            onConfirm(selection)
                            onDismissRequest()
                        },
                        text = stringResource(R.string.confirm),
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.textButtonColorsPrimary()
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    )
}
