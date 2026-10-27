package io.github.martyjre.sudoku.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import io.github.martyjre.sudoku.engine.CELLS
import io.github.martyjre.sudoku.engine.SIZE
import io.github.martyjre.sudoku.engine.Units
import io.github.martyjre.sudoku.game.UiState

@Composable
fun Board(state: UiState, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalBoardColors.current
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val game = state.game
    val selected = state.selected
    val selectedDigit = selected?.let { game.values[it] }?.takeIf { it != 0 }

    Canvas(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = "Sudoku board" }
            .pointerInput(onSelect) {
                detectTapGestures { offset ->
                    val col = (offset.x / (size.width / SIZE.toFloat())).toInt().coerceIn(0, SIZE - 1)
                    val row = (offset.y / (size.height / SIZE.toFloat())).toInt().coerceIn(0, SIZE - 1)
                    onSelect(row * SIZE + col)
                }
            }
    ) {
        val cellSize = size.width / SIZE
        drawRect(colors.background)

        for (cell in 0 until CELLS) {
            val digit = game.values[cell]
            val fill = when {
                cell == selected -> colors.selected
                cell in game.conflicts -> colors.conflict
                selectedDigit != null && digit == selectedDigit -> colors.sameDigit
                selected != null && Units.sees(cell, selected) -> colors.peer
                else -> null
            }
            if (fill != null) drawRect(fill, topLeft = cellOrigin(cell, cellSize), size = Size(cellSize, cellSize))
        }

        val digitStyle = TextStyle(fontSize = (cellSize * 0.58f).toSp())
        val noteStyle = TextStyle(fontSize = (cellSize * 0.24f).toSp())
        for (cell in 0 until CELLS) {
            val origin = cellOrigin(cell, cellSize)
            val digit = game.values[cell]
            if (digit != 0) {
                val color = when {
                    cell in game.conflicts && !game.isGiven(cell) -> colors.error
                    game.isGiven(cell) -> colors.given
                    else -> colors.entered
                }
                val weight = if (game.isGiven(cell)) FontWeight.SemiBold else FontWeight.Normal
                drawCentered(measurer, digit.toString(), digitStyle.copy(color = color, fontWeight = weight), origin, cellSize)
            } else if (game.notes[cell] != 0) {
                val noteSize = cellSize / 3
                for (d in 1..SIZE) {
                    if (!game.hasNote(cell, d)) continue
                    val noteOrigin = origin + Offset((d - 1) % 3 * noteSize, (d - 1) / 3 * noteSize)
                    val highlight = d == selectedDigit
                    val style = noteStyle.copy(
                        color = if (highlight) colors.entered else colors.notes,
                        fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                    )
                    drawCentered(measurer, d.toString(), style, noteOrigin, noteSize)
                }
            }
        }

        for (i in 0..SIZE) {
            val thick = i % 3 == 0
            val color = if (thick) colors.thickLine else colors.thinLine
            val stroke = if (thick) 2.dp.toPx() else 1.dp.toPx()
            val pos = (i * cellSize).coerceIn(stroke / 2, size.width - stroke / 2)
            drawLine(color, Offset(pos, 0f), Offset(pos, size.height), stroke)
            drawLine(color, Offset(0f, pos), Offset(size.width, pos), stroke)
        }
    }
}

private fun cellOrigin(cell: Int, cellSize: Float) = Offset(cell % SIZE * cellSize, cell / SIZE * cellSize)

private fun DrawScope.drawCentered(measurer: TextMeasurer, text: String, style: TextStyle, origin: Offset, box: Float) {
    val layout = measurer.measure(text, style)
    drawText(
        layout,
        topLeft = origin + Offset((box - layout.size.width) / 2, (box - layout.size.height) / 2),
    )
}

