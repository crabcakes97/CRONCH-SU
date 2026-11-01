package me.weishu.kernelsu.ui.screen.home

import android.app.ActivityManager
import android.content.Context
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.theme.CronchCardShape
import me.weishu.kernelsu.ui.theme.CronchCoopBrown
import me.weishu.kernelsu.ui.theme.CronchDisplayFontFamily
import me.weishu.kernelsu.ui.theme.CronchDoritoOrange
import me.weishu.kernelsu.ui.theme.CronchPillShape
import me.weishu.kernelsu.ui.theme.CronchRanchCream
import me.weishu.kernelsu.ui.theme.CronchSlayPink
import me.weishu.kernelsu.ui.theme.cronchPanelColor
import me.weishu.kernelsu.ui.theme.cronchPanelContentColor
import me.weishu.kernelsu.ui.theme.isInDarkTheme
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import java.io.File
import kotlin.math.roundToInt

/**
 * The CRONCHOMETER: the instrument that measures CRONCH intensity, reading
 * the phone's real state. RAM comes from ActivityManager.MemoryInfo; CPU is
 * overall usage sampled from /proc/stat deltas. Nothing is estimated or
 * animated for show: a value that cannot be read renders as unavailable,
 * and the bars only ever move on real readings.
 */
private data class CronchometerReadings(
    val ramUsedBytes: Long? = null,
    val ramTotalBytes: Long? = null,
    val cpuPercent: Float? = null,
    val cpuAvailable: Boolean = true,
)

private data class CpuTimes(val idle: Long, val total: Long)

private fun readCpuTimes(): CpuTimes? {
    return try {
        val line = File("/proc/stat").bufferedReader().useLines { lines ->
            lines.firstOrNull { it.startsWith("cpu ") }
        } ?: return null
        val fields = line.trim().split(Regex("\\s+")).drop(1).mapNotNull { it.toLongOrNull() }
        if (fields.size < 4) return null
        val idle = fields[3] + if (fields.size > 4) fields[4] else 0L
        CpuTimes(idle = idle, total = fields.sum())
    } catch (_: Exception) {
        null
    }
}

private fun readRam(context: Context): Pair<Long?, Long?> {
    return try {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        Pair(info.totalMem - info.availMem, info.totalMem)
    } catch (_: Exception) {
        Pair(null, null)
    }
}

private const val CRONCHOMETER_INTERVAL_MS = 2_000L

@Composable
private fun rememberCronchometerReadings(): CronchometerReadings {
    val context = LocalContext.current
    var readings by remember { mutableStateOf(CronchometerReadings()) }
    val scope = rememberCoroutineScope()
    var samplingJob by remember { mutableStateOf<Job?>(null) }

    // Sampling runs only while the home screen is resumed: the job is
    // started on resume and cancelled on pause/dispose, so nothing polls
    // while the coop is in the background.
    LifecycleResumeEffect(Unit) {
        samplingJob = scope.launch(Dispatchers.Default) {
            var prev: CpuTimes? = null
            var cpuFailures = 0
            while (isActive) {
                val (ramUsed, ramTotal) = readRam(context)
                val now = readCpuTimes()
                var cpu: Float? = null
                if (now == null) {
                    cpuFailures++
                } else {
                    cpuFailures = 0
                    val previous = prev
                    if (previous != null && now.total > previous.total) {
                        val idleDelta = now.idle - previous.idle
                        val totalDelta = now.total - previous.total
                        cpu = ((1.0 - idleDelta.toDouble() / totalDelta.toDouble()) * 100)
                            .toFloat().coerceIn(0f, 100f)
                    }
                    prev = now
                }
                readings = CronchometerReadings(
                    ramUsedBytes = ramUsed,
                    ramTotalBytes = ramTotal,
                    cpuPercent = cpu,
                    cpuAvailable = cpuFailures < 2,
                )
                delay(CRONCHOMETER_INTERVAL_MS)
            }
        }
        onPauseOrDispose { samplingJob?.cancel() }
    }
    return readings
}

@Composable
private fun CronchometerMeterMaterial(
    label: String,
    valueText: String,
    fraction: Float?,
    fillColor: Color,
    trackColor: Color,
    valueColor: Color,
    labelColor: Color,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = (fraction ?: 0f).coerceIn(0f, 1f),
        label = "CronchometerFill"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = labelColor,
            )
            Text(
                text = valueText,
                style = MaterialTheme.typography.bodyMedium,
                color = valueColor,
            )
        }
        if (fraction != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(trackColor, CronchPillShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(10.dp)
                        .background(fillColor, CronchPillShape)
                )
            }
        }
    }
}

@Composable
private fun CronchometerMeterMiuix(
    label: String,
    valueText: String,
    fraction: Float?,
    fillColor: Color,
    trackColor: Color,
    valueColor: Color,
    labelColor: Color,
) {
    val animatedFraction by animateFloatAsState(
        targetValue = (fraction ?: 0f).coerceIn(0f, 1f),
        label = "CronchometerFill"
    )
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            top.yukonga.miuix.kmp.basic.Text(
                text = label,
                fontSize = 16.sp,
                color = labelColor,
            )
            top.yukonga.miuix.kmp.basic.Text(
                text = valueText,
                fontSize = 14.sp,
                color = valueColor,
            )
        }
        if (fraction != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(trackColor, CronchPillShape)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(10.dp)
                        .background(fillColor, CronchPillShape)
                )
            }
        }
    }
}

@Composable
fun CronchometerSectionMaterial(
    modifier: Modifier = Modifier,
) {
    val readings = rememberCronchometerReadings()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.cronchometer_title),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = CronchDisplayFontFamily
            ),
            color = CronchDoritoOrange,
            modifier = Modifier.padding(start = 4.dp),
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CronchRanchCream,
            contentColor = CronchCoopBrown,
            shape = CronchCardShape,
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.cronchometer_prophecy_line),
                    style = MaterialTheme.typography.bodySmall,
                    color = CronchCoopBrown.copy(alpha = 0.7f),
                )
                val ramFraction = if (readings.ramUsedBytes != null && readings.ramTotalBytes != null
                    && readings.ramTotalBytes > 0
                ) {
                    readings.ramUsedBytes.toFloat() / readings.ramTotalBytes.toFloat()
                } else {
                    null
                }
                val ramValue = if (readings.ramUsedBytes != null && readings.ramTotalBytes != null) {
                    stringResource(
                        R.string.cronchometer_ram_value,
                        readings.ramUsedBytes / 1e9,
                        readings.ramTotalBytes / 1e9,
                    )
                } else {
                    stringResource(R.string.cronchometer_cpu_warming)
                }
                CronchometerMeterMaterial(
                    label = stringResource(R.string.cronchometer_ram_label),
                    valueText = ramValue,
                    fraction = ramFraction,
                    fillColor = CronchDoritoOrange,
                    trackColor = CronchCoopBrown.copy(alpha = 0.15f),
                    valueColor = CronchCoopBrown,
                    labelColor = CronchCoopBrown,
                )
                val cpuValue = when {
                    !readings.cpuAvailable -> stringResource(R.string.cronchometer_cpu_unavailable)
                    readings.cpuPercent != null -> stringResource(
                        R.string.cronchometer_cpu_value,
                        readings.cpuPercent.roundToInt()
                    )
                    else -> stringResource(R.string.cronchometer_cpu_warming)
                }
                CronchometerMeterMaterial(
                    label = stringResource(R.string.cronchometer_cpu_label),
                    valueText = cpuValue,
                    fraction = readings.cpuPercent?.let { it / 100f },
                    fillColor = CronchSlayPink,
                    trackColor = CronchCoopBrown.copy(alpha = 0.15f),
                    valueColor = CronchCoopBrown,
                    labelColor = CronchCoopBrown,
                )
            }
        }
    }
}

@Composable
fun CronchometerSectionMiuix(
    modifier: Modifier = Modifier,
) {
    val readings = rememberCronchometerReadings()
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        top.yukonga.miuix.kmp.basic.Text(
            text = stringResource(R.string.cronchometer_title),
            // Miuix Text takes no fontFamily parameter; size + weight carry the title styling.
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = CronchDoritoOrange,
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = cronchPanelColor(isInDarkTheme()),
                contentColor = cronchPanelContentColor(isInDarkTheme()),
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                top.yukonga.miuix.kmp.basic.Text(
                    text = stringResource(R.string.cronchometer_prophecy_line),
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariantSummary,
                )
                val ramFraction = if (readings.ramUsedBytes != null && readings.ramTotalBytes != null
                    && readings.ramTotalBytes > 0
                ) {
                    readings.ramUsedBytes.toFloat() / readings.ramTotalBytes.toFloat()
                } else {
                    null
                }
                val ramValue = if (readings.ramUsedBytes != null && readings.ramTotalBytes != null) {
                    stringResource(
                        R.string.cronchometer_ram_value,
                        readings.ramUsedBytes / 1e9,
                        readings.ramTotalBytes / 1e9,
                    )
                } else {
                    stringResource(R.string.cronchometer_cpu_warming)
                }
                val panelContent = cronchPanelContentColor(isInDarkTheme())
                CronchometerMeterMiuix(
                    label = stringResource(R.string.cronchometer_ram_label),
                    valueText = ramValue,
                    fraction = ramFraction,
                    fillColor = CronchDoritoOrange,
                    trackColor = panelContent.copy(alpha = 0.15f),
                    valueColor = panelContent,
                    labelColor = panelContent,
                )
                val cpuValue = when {
                    !readings.cpuAvailable -> stringResource(R.string.cronchometer_cpu_unavailable)
                    readings.cpuPercent != null -> stringResource(
                        R.string.cronchometer_cpu_value,
                        readings.cpuPercent.roundToInt()
                    )
                    else -> stringResource(R.string.cronchometer_cpu_warming)
                }
                CronchometerMeterMiuix(
                    label = stringResource(R.string.cronchometer_cpu_label),
                    valueText = cpuValue,
                    fraction = readings.cpuPercent?.let { it / 100f },
                    fillColor = CronchSlayPink,
                    trackColor = panelContent.copy(alpha = 0.15f),
                    valueColor = panelContent,
                    labelColor = panelContent,
                )
            }
        }
    }
}
