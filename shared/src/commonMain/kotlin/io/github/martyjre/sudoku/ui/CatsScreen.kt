package io.github.martyjre.sudoku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.martyjre.sudoku.cats.CatsController
import io.github.martyjre.sudoku.cats.CatsGame
import io.github.martyjre.sudoku.cats.Mark
import kotlinx.coroutines.delay

@Composable
fun CatsScreen(
    controller: CatsController,
    onBack: () -> Unit,
    onNewGame: () -> Unit,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val game = state.game
    val colors = MaterialTheme.colorScheme
    val haptics = LocalHapticFeedback.current

    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val running = lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && !game.isSolved && !state.paused
    LaunchedEffect(controller, running) {
        while (running) {
            delay(1000)
            controller.tick()
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) { Icon(AppIcons.Back, contentDescription = "Back") }
                Text("Cats · ${game.size}×${game.size}", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(16.dp))
                Text(
                    formatTime(game.elapsedSeconds),
                    style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
                    color = colors.onSurfaceVariant,
                )
                IconButton(onClick = controller::togglePause, enabled = !game.isSolved) {
                    Icon(
                        if (state.paused) AppIcons.Play else AppIcons.Pause,
                        contentDescription = if (state.paused) "Resume" else "Pause",
                        tint = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = controller::undo, enabled = state.canUndo && !state.paused) { Text("Undo") }
                TextButton(onClick = controller::hint, enabled = !state.paused) { Text("Hint") }
            }
            Spacer(Modifier.height(8.dp))

            Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
                    if (state.paused) {
                        Box(
                            Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)).background(colors.surfaceVariant),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Paused", style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = controller::togglePause) { Text("Resume") }
                            }
                        }
                    } else {
                        CatsBoard(
                            game = game,
                            onTap = {
                                controller.tap(it)
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            },
                            onDrag = controller::dragOver,
                            onDragEnd = controller::endDrag,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }

                Text(
                    "One cat in every row, column and color. Cats never touch, not even diagonally. " +
                        "Tap once for X, twice for a cat; drag to mark many X.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !state.paused, onClick = controller::toggleAutoX)
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Auto-mark X around cats", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Switch(checked = game.autoX, onCheckedChange = { controller.toggleAutoX() }, enabled = !state.paused)
                }
            }
        }

        if (game.isSolved) {
            SolvedOverlay(
                seconds = game.elapsedSeconds,
                hints = game.hintsUsed,
                difficulty = game.puzzle.difficulty.label,
                onNewGame = onNewGame,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun CatsBoard(
    game: CatsGame,
    onTap: (Int) -> Unit,
    onDrag: (Int) -> Unit,
    onDragEnd: () -> Unit,
    modifier: Modifier,
) {
    val colors = LocalBoardColors.current
    val n = game.size

    fun cellAt(offset: Offset, width: Int): Int? {
        val cellSize = width / n.toFloat()
        val col = (offset.x / cellSize).toInt()
        val row = (offset.y / cellSize).toInt()
        return if (row in 0 until n && col in 0 until n) row * n + col else null
    }

    Canvas(
        modifier
            .semantics { contentDescription = "Cats board, ${game.cats.size} of $n cats placed" }
            .pointerInput(n) { detectTapGestures { offset -> cellAt(offset, size.width)?.let(onTap) } }
            .pointerInput(n) {
                detectDragGestures(
                    onDragStart = { offset -> cellAt(offset, size.width)?.let(onDrag) },
                    onDragEnd = onDragEnd,
                    onDragCancel = onDragEnd,
                    onDrag = { change, _ -> cellAt(change.position, size.width)?.let(onDrag) },
                )
            }
    ) {
        val cell = size.width / n
        val regions = game.puzzle.regions
        for (i in 0 until n * n) {
            val origin = Offset(i % n * cell, i / n * cell)
            drawRect(colors.regions[regions[i] % colors.regions.size], origin, Size(cell, cell))
            when (game.marks[i]) {
                Mark.X -> drawX(origin, cell, colors.xMark)
                Mark.CAT -> drawCat(origin, cell, if (i in game.conflicts) colors.error else colors.catInk)
                Mark.EMPTY -> Unit
            }
        }

        val thin = 1.dp.toPx()
        val thick = 3.dp.toPx()
        for (i in 1 until n) {
            drawLine(colors.background.copy(alpha = 0.5f), Offset(i * cell, 0f), Offset(i * cell, size.height), thin)
            drawLine(colors.background.copy(alpha = 0.5f), Offset(0f, i * cell), Offset(size.width, i * cell), thin)
        }
        // Region borders are drawn per edge so each color blob gets a clear outline.
        for (r in 0 until n) for (c in 0 until n) {
            val i = r * n + c
            if (c < n - 1 && regions[i] != regions[i + 1]) {
                drawLine(colors.thickLine, Offset((c + 1) * cell, r * cell), Offset((c + 1) * cell, (r + 1) * cell), thick, StrokeCap.Square)
            }
            if (r < n - 1 && regions[i] != regions[i + n]) {
                drawLine(colors.thickLine, Offset(c * cell, (r + 1) * cell), Offset((c + 1) * cell, (r + 1) * cell), thick, StrokeCap.Square)
            }
        }
        drawRect(colors.thickLine, size = size, style = androidx.compose.ui.graphics.drawscope.Stroke(thick * 1.5f))
    }
}

private fun DrawScope.drawX(origin: Offset, cell: Float, color: Color) {
    val inset = cell * 0.34f
    val stroke = cell * 0.07f
    drawLine(color, origin + Offset(inset, inset), origin + Offset(cell - inset, cell - inset), stroke, StrokeCap.Round)
    drawLine(color, origin + Offset(cell - inset, inset), origin + Offset(inset, cell - inset), stroke, StrokeCap.Round)
}

private fun DrawScope.drawCat(origin: Offset, cell: Float, color: Color) {
    val cx = origin.x + cell / 2
    val cy = origin.y + cell * 0.56f
    val r = cell * 0.27f
    val ears = Path().apply {
        moveTo(cx - r * 0.95f, cy - r * 0.15f)
        lineTo(cx - r * 0.8f, cy - r * 1.45f)
        lineTo(cx - r * 0.1f, cy - r * 0.8f)
        close()
        moveTo(cx + r * 0.95f, cy - r * 0.15f)
        lineTo(cx + r * 0.8f, cy - r * 1.45f)
        lineTo(cx + r * 0.1f, cy - r * 0.8f)
        close()
    }
    drawPath(ears, color)
    drawCircle(color, r, Offset(cx, cy))
    val eye = Color.White.copy(alpha = 0.92f).takeIf { color.luminanceIsDark() } ?: Color(0xFF1E2128)
    drawCircle(eye, r * 0.17f, Offset(cx - r * 0.38f, cy - r * 0.08f))
    drawCircle(eye, r * 0.17f, Offset(cx + r * 0.38f, cy - r * 0.08f))
    drawCircle(eye, r * 0.1f, Offset(cx, cy + r * 0.28f))
}

private fun Color.luminanceIsDark() = red * 0.299f + green * 0.587f + blue * 0.114f < 0.5f
