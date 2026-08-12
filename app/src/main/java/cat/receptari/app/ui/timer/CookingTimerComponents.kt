package cat.receptari.app.ui.timer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cat.receptari.app.R
import cat.receptari.app.core.designsystem.FilterPill
import cat.receptari.app.core.designsystem.Hairline
import cat.receptari.app.core.designsystem.OrnamentHeading
import cat.receptari.app.core.designsystem.PaperCard
import cat.receptari.app.core.designsystem.paperGrain
import cat.receptari.app.core.designsystem.theme.ReceptariTheme
import cat.receptari.app.core.designsystem.theme.ReceptariTextStyles
import cat.receptari.app.domain.model.CookingTimer
import cat.receptari.app.domain.model.CookingTimerStatus
import cat.receptari.app.domain.timer.TimerMath
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ActiveTimerDock(
    timers: List<CookingTimer>,
    remainingSeconds: Map<String, Long>,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit,
    onAddMinute: (String) -> Unit,
    onCancel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .paperGrain()
            .navigationBarsPadding(),
    ) {
        Hairline()
        Text(
            text = pluralStringResource(R.plurals.timer_dock_count, timers.size, timers.size),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(timers, key = { it.id }) { timer ->
                TimerCard(
                    timer = timer,
                    remainingSeconds = remainingSeconds[timer.id] ?: 0L,
                    onPause = { onPause(timer.id) },
                    onResume = { onResume(timer.id) },
                    onAddMinute = { onAddMinute(timer.id) },
                    onCancel = { onCancel(timer.id) },
                )
            }
        }
    }
}

@Composable
private fun TimerCard(
    timer: CookingTimer,
    remainingSeconds: Long,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onAddMinute: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PaperCard(
        modifier = modifier.width(330.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Timer,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = timer.label,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = if (timer.status == CookingTimerStatus.PAUSED) {
                        stringResource(R.string.timer_paused_time, TimerMath.format(remainingSeconds))
                    } else {
                        TimerMath.format(remainingSeconds) // i18n-exempt: digital timer numerals
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            IconButton(onClick = onAddMinute) {
                Icon(Icons.Default.Add, stringResource(R.string.timer_add_minute))
            }
            IconButton(
                onClick = if (timer.status == CookingTimerStatus.RUNNING) onPause else onResume,
            ) {
                Icon(
                    imageVector = if (timer.status == CookingTimerStatus.RUNNING) {
                        Icons.Default.Pause
                    } else {
                        Icons.Default.PlayArrow
                    },
                    contentDescription = stringResource(
                        if (timer.status == CookingTimerStatus.RUNNING) {
                            R.string.timer_pause
                        } else {
                            R.string.timer_resume
                        },
                    ),
                )
            }
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, stringResource(R.string.timer_cancel))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerSetupSheet(
    title: String,
    initialMinutes: Int?,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var minutes by remember(title, initialMinutes) {
        mutableIntStateOf((initialMinutes ?: DEFAULT_TIMER_MINUTES).coerceIn(1, MAX_TIMER_MINUTES))
    }

    ModalBottomSheet(
        modifier = modifier,
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .paperGrain(intensity = 1f)
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Hairline(modifier = Modifier.width(54.dp))
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .paperGrain(intensity = 1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OrnamentHeading(
                    title = stringResource(R.string.timer_setup_title),
                )
                Text(
                    text = title,
                    style = ReceptariTextStyles.Aside,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.secondary,
                )
                PocketWatch(minutes = minutes)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(onClick = { minutes = (minutes - 5).coerceAtLeast(1) }) {
                        Text(stringResource(R.string.timer_decrease_five_minutes))
                    }
                    OutlinedIconButton(
                        onClick = { minutes = (minutes - 1).coerceAtLeast(1) },
                    ) {
                        Icon(Icons.Default.Remove, stringResource(R.string.timer_decrease_minute))
                    }
                    Column(
                        modifier = Modifier.width(72.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            text = minutes.toString(), // i18n-exempt: timer numeral
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = stringResource(R.string.timer_minutes_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    OutlinedIconButton(
                        onClick = { minutes = (minutes + 1).coerceAtMost(MAX_TIMER_MINUTES) },
                    ) {
                        Icon(Icons.Default.Add, stringResource(R.string.timer_increase_minute))
                    }
                    TextButton(
                        onClick = { minutes = (minutes + 5).coerceAtMost(MAX_TIMER_MINUTES) },
                    ) {
                        Text(stringResource(R.string.timer_increase_five_minutes))
                    }
                }
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(TIMER_PRESETS, key = { it }) { preset ->
                        FilterPill(
                            label = stringResource(R.string.common_minutes_short, preset),
                            selected = minutes == preset,
                            onClick = { minutes = preset },
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    Button(
                        onClick = { onStart(minutes) },
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text(stringResource(R.string.timer_start))
                    }
                }
            }
        }
    }
}

@Composable
private fun PocketWatch(minutes: Int, modifier: Modifier = Modifier) {
    val rule = ReceptariTheme.palette.rule
    val ink = MaterialTheme.colorScheme.primary
    val minuteHandDegrees by animateFloatAsState(
        targetValue = minutes * DEGREES_PER_MINUTE,
        animationSpec = tween(HAND_ANIMATION_MILLIS),
        label = "minute-hand",
    )
    val hourHandDegrees by animateFloatAsState(
        targetValue = minutes * DEGREES_PER_HOUR_MINUTE,
        animationSpec = tween(HAND_ANIMATION_MILLIS),
        label = "hour-hand",
    )
    Canvas(modifier = modifier.size(190.dp)) {
            val center = Offset(size.width / 2f, size.height * 0.56f)
            val radius = size.minDimension * 0.39f
            val faceTop = center.y - radius
            val bowRadius = radius * 0.19f

            // The bow and crown give the dial the silhouette of an old pocket watch.
            drawCircle(
                color = rule,
                radius = bowRadius,
                center = Offset(center.x, faceTop - bowRadius),
                style = Stroke(3.dp.toPx()),
            )
            drawLine(
                color = rule,
                start = Offset(center.x - radius * 0.13f, faceTop + 2.dp.toPx()),
                end = Offset(center.x + radius * 0.13f, faceTop + 2.dp.toPx()),
                strokeWidth = 4.dp.toPx(),
            )
            drawCircle(rule, radius = radius, center = center, style = Stroke(2.dp.toPx()))
            drawCircle(rule.copy(alpha = 0.55f), radius = radius - 6.dp.toPx(), center = center, style = Stroke(1.dp.toPx()))

            repeat(60) { tick ->
                val angle = tick * (2.0 * PI / 60.0) - PI / 2.0
                val outer = radius - 10.dp.toPx()
                val inner = outer - if (tick % 5 == 0) 10.dp.toPx() else 5.dp.toPx()
                drawLine(
                    color = if (tick % 5 == 0) ink else rule,
                    start = Offset(
                        center.x + cos(angle).toFloat() * inner,
                        center.y + sin(angle).toFloat() * inner,
                    ),
                    end = Offset(
                        center.x + cos(angle).toFloat() * outer,
                        center.y + sin(angle).toFloat() * outer,
                    ),
                    strokeWidth = if (tick % 5 == 0) 2.dp.toPx() else 1.dp.toPx(),
                )
            }

            fun handEnd(degrees: Float, length: Float): Offset {
                val angle = Math.toRadians((degrees - 90f).toDouble())
                return Offset(
                    x = center.x + cos(angle).toFloat() * length,
                    y = center.y + sin(angle).toFloat() * length,
                )
            }

            drawLine(
                color = rule,
                start = center,
                end = handEnd(hourHandDegrees, radius * 0.48f),
                strokeWidth = 5.dp.toPx(),
            )
            drawLine(
                color = ink,
                start = center,
                end = handEnd(minuteHandDegrees, radius * 0.7f),
                strokeWidth = 3.dp.toPx(),
            )
            drawCircle(color = ink, radius = 5.dp.toPx(), center = center)
    }
}

private val TIMER_PRESETS = listOf(5, 10, 15, 30, 45, 60)
private const val DEFAULT_TIMER_MINUTES = 5
private const val MAX_TIMER_MINUTES = 24 * 60
private const val DEGREES_PER_MINUTE = 6f
private const val DEGREES_PER_HOUR_MINUTE = 0.5f
private const val HAND_ANIMATION_MILLIS = 320
