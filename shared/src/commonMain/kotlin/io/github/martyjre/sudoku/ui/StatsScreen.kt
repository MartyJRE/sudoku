package io.github.martyjre.sudoku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.martyjre.sudoku.engine.Difficulty
import io.github.martyjre.sudoku.storage.GameKind
import io.github.martyjre.sudoku.storage.GameRecord
import io.github.martyjre.sudoku.storage.Summary
import io.github.martyjre.sudoku.storage.date
import io.github.martyjre.sudoku.storage.summarize
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

@Composable
fun StatsScreen(history: List<GameRecord>, onBack: () -> Unit) {
    var kind by remember { mutableStateOf(GameKind.SUDOKU) }
    var difficulty by remember { mutableStateOf<Difficulty?>(null) }
    val zone = remember { TimeZone.currentSystemDefault() }
    val today = remember { Clock.System.todayIn(zone) }
    val summary = remember(history, kind, difficulty) { summarize(history, kind, difficulty, today, zone) }
    val colors = MaterialTheme.colorScheme

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(AppIcons.Back, contentDescription = "Back") }
            Text("Stats", style = MaterialTheme.typography.titleLarge)
        }

        Segmented(
            options = GameKind.entries.map { it.label },
            selected = kind.ordinal,
            onSelect = { kind = GameKind.entries[it] },
        )
        Spacer(Modifier.height(12.dp))
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(selected = difficulty == null, onClick = { difficulty = null }, label = { Text("All") })
            for (d in Difficulty.entries) {
                FilterChip(selected = difficulty == d, onClick = { difficulty = d }, label = { Text(d.label) })
            }
        }
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatTile("Solved", summary.solved.toString(), Modifier.weight(1f))
            StatTile("Best", summary.bestSeconds?.let(::formatTime) ?: "–", Modifier.weight(1f))
            StatTile("Average", summary.averageSeconds?.let(::formatTime) ?: "–", Modifier.weight(1f))
            StatTile("Streak", "${summary.currentStreak}d", Modifier.weight(1f), caption = "best ${summary.longestStreak}d")
        }
        Spacer(Modifier.height(24.dp))

        if (summary.solved == 0) {
            Text(
                "Solve a ${kind.label.lowercase()} puzzle and your charts show up here.",
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 32.dp),
            )
        } else {
            ChartCard(if (summary.recent.size == 1) "Solve time" else "Solve time, last ${summary.recent.size} games") { SolveTimeChart(summary, zone) }
            Spacer(Modifier.height(16.dp))
            ChartCard("Solved per day, last 30 days") { PerDayChart(summary) }
        }
        Spacer(Modifier.height(16.dp))
        DifficultyTable(summary)
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
internal fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(22.dp))
            .padding(4.dp),
    ) {
        options.forEachIndexed { index, label ->
            val active = index == selected
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) colors.primary else Color.Transparent)
                    .clickable { onSelect(index) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) colors.onPrimary else colors.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier, caption: String? = null) {
    Surface(shape = RoundedCornerShape(14.dp), tonalElevation = 1.dp, modifier = modifier) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = "tnum"), fontWeight = FontWeight.SemiBold)
            if (caption != null) {
                Text(caption, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ChartCard(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            content()
        }
    }
}

/** Single-series line of solve times with an average reference line; tap a point for its details. */
@Composable
private fun SolveTimeChart(summary: Summary, zone: TimeZone) {
    val board = LocalBoardColors.current
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val points = summary.recent
    var selected by remember(points) { mutableStateOf(points.lastIndex) }
    val pick = points.getOrNull(selected)
    val axisStyle = TextStyle(fontSize = 11.sp, color = colors.onSurfaceVariant)

    Text(
        pick?.let { "${formatTime(it.seconds)} · ${it.difficulty.label} · ${formatDate(it.date(zone))}" } ?: " ",
        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
        color = colors.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))

    val maxSeconds = niceCeiling(points.maxOf { it.seconds })
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(180.dp)
            .semantics { contentDescription = "Solve time chart, ${points.size} games" }
            .pointerInput(points) {
                detectTapGestures { offset ->
                    val left = 44.dp.toPx()
                    val step = if (points.size > 1) (size.width - left - 8.dp.toPx()) / (points.size - 1) else 0f
                    selected = if (step == 0f) 0 else ((offset.x - left) / step).let { kotlin.math.round(it).toInt() }.coerceIn(0, points.lastIndex)
                }
            }
    ) {
        val left = 44.dp.toPx()
        val right = size.width - 8.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 8.dp.toPx()
        fun y(seconds: Long) = bottom - (bottom - top) * seconds / maxSeconds.toFloat()
        fun x(i: Int) = if (points.size == 1) (left + right) / 2 else left + (right - left) * i / (points.size - 1)

        for (tick in listOf(0L, maxSeconds / 2, maxSeconds)) {
            drawLine(board.chartGrid, Offset(left, y(tick)), Offset(right, y(tick)), 1.dp.toPx())
            val label = measurer.measure(formatTime(tick), axisStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 6.dp.toPx(), y(tick) - label.size.height / 2))
        }

        summary.averageSeconds?.let { avg ->
            drawLine(
                colors.onSurfaceVariant.copy(alpha = 0.6f),
                Offset(left, y(avg)), Offset(right, y(avg)), 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
            val label = measurer.measure("avg", axisStyle)
            drawText(label, topLeft = Offset(right - label.size.width, y(avg) - label.size.height - 2.dp.toPx()))
        }

        if (points.size > 1) {
            val path = Path().apply {
                points.forEachIndexed { i, p -> if (i == 0) moveTo(x(i), y(p.seconds)) else lineTo(x(i), y(p.seconds)) }
            }
            drawPath(path, board.chartSeries, style = Stroke(2.dp.toPx()))
        }
        val surface = colors.surface
        points.forEachIndexed { i, p ->
            val center = Offset(x(i), y(p.seconds))
            val radius = if (i == selected) 6.dp.toPx() else 4.dp.toPx()
            drawCircle(surface, radius + 2.dp.toPx(), center)
            drawCircle(board.chartSeries, radius, center)
        }
        if (pick != null) {
            drawLine(colors.onSurfaceVariant.copy(alpha = 0.4f), Offset(x(selected), top), Offset(x(selected), bottom), 1.dp.toPx())
        }
    }
}

/** Columns of solves per day; tap a column for its date and count. */
@Composable
private fun PerDayChart(summary: Summary) {
    val board = LocalBoardColors.current
    val colors = MaterialTheme.colorScheme
    val measurer = rememberTextMeasurer()
    val days = summary.perDay
    var selected by remember(days) { mutableStateOf(days.lastIndex) }
    val pick = days.getOrNull(selected)
    val maxCount = maxOf(1, days.maxOf { it.count })
    val axisStyle = TextStyle(fontSize = 11.sp, color = colors.onSurfaceVariant)

    Text(
        pick?.let { "${formatDate(it.date)} · ${it.count} solved" } ?: " ",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(150.dp)
            .semantics { contentDescription = "Solved per day chart, last ${days.size} days" }
            .pointerInput(days) {
                detectTapGestures { offset ->
                    val left = 28.dp.toPx()
                    val slot = (size.width - left) / days.size
                    selected = ((offset.x - left) / slot).toInt().coerceIn(0, days.lastIndex)
                }
            }
    ) {
        val left = 28.dp.toPx()
        val top = 8.dp.toPx()
        val bottom = size.height - 20.dp.toPx()
        val slot = (size.width - left) / days.size
        val gap = 2.dp.toPx()
        val radius = 4.dp.toPx()

        for (tick in setOf(0, maxCount)) {
            val y = bottom - (bottom - top) * tick / maxCount
            drawLine(board.chartGrid, Offset(left, y), Offset(size.width, y), 1.dp.toPx())
            val label = measurer.measure(tick.toString(), axisStyle)
            drawText(label, topLeft = Offset(left - label.size.width - 6.dp.toPx(), y - label.size.height / 2))
        }

        days.forEachIndexed { i, day ->
            if (day.count == 0) return@forEachIndexed
            val height = (bottom - top) * day.count / maxCount
            val x = left + i * slot + gap / 2
            val alpha = if (i == selected) 1f else 0.75f
            // Rounded top, square base: draw a rounded rect and square off its bottom corners.
            drawRoundRect(board.chartSeries.copy(alpha = alpha), Offset(x, bottom - height), Size(slot - gap, height), CornerRadius(radius))
            drawRect(board.chartSeries.copy(alpha = alpha), Offset(x, bottom - minOf(radius, height)), Size(slot - gap, minOf(radius, height)))
        }
        if (pick != null) {
            val x = left + selected * slot + slot / 2
            drawLine(colors.onSurfaceVariant.copy(alpha = 0.4f), Offset(x, top), Offset(x, bottom), 1.dp.toPx())
        }

        val first = measurer.measure(formatDate(days.first().date), axisStyle)
        val last = measurer.measure("Today", axisStyle)
        drawText(first, topLeft = Offset(left, bottom + 4.dp.toPx()))
        drawText(last, topLeft = Offset(size.width - last.size.width, bottom + 4.dp.toPx()))
    }
}

@Composable
private fun DifficultyTable(summary: Summary) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            TableRow("Difficulty", "Solved", "Best", "Average", header = true)
            for (row in summary.byDifficulty) {
                HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))
                TableRow(
                    row.difficulty.label,
                    row.solved.toString(),
                    row.bestSeconds?.let(::formatTime) ?: "–",
                    row.averageSeconds?.let(::formatTime) ?: "–",
                )
            }
        }
    }
}

@Composable
private fun TableRow(vararg cells: String, header: Boolean = false) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        cells.forEachIndexed { i, text ->
            Text(
                text,
                modifier = Modifier.weight(if (i == 0) 1.4f else 1f),
                style = (if (header) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodyMedium)
                    .copy(fontFeatureSettings = "tnum"),
                color = if (header) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatDate(date: LocalDate) = "${date.day} ${monthNames[date.month.ordinal]}"

/** Rounds the axis maximum up to a whole number of minutes so the tick labels stay readable. */
private fun niceCeiling(seconds: Long): Long {
    val minutes = (seconds + 59) / 60
    val step = when {
        minutes <= 2 -> 1
        minutes <= 10 -> 2
        minutes <= 30 -> 5
        else -> 10
    }
    return maxOf(60L, ((minutes + step - 1) / step) * step * 60)
}
