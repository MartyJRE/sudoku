package io.github.martyjre.sudoku.game

import io.github.martyjre.sudoku.engine.CELLS
import io.github.martyjre.sudoku.engine.Puzzle
import io.github.martyjre.sudoku.engine.SIZE
import io.github.martyjre.sudoku.engine.Units
import io.github.martyjre.sudoku.engine.bit
import kotlinx.serialization.Serializable

@Serializable
data class GameState(
    val puzzle: Puzzle,
    val values: List<Int> = puzzle.givens,
    /** Pencil marks per cell as a digit bitmask. */
    val notes: List<Int> = List(CELLS) { 0 },
    val elapsedSeconds: Long = 0,
    val hintsUsed: Int = 0,
    val autoCandidates: Boolean = false,
) {
    val isSolved: Boolean get() = values == puzzle.solution

    fun isGiven(cell: Int) = puzzle.givens[cell] != 0

    fun hasNote(cell: Int, digit: Int) = notes[cell] and bit(digit) != 0

    /** Cells whose digit repeats somewhere in their row, column or box. */
    val conflicts: Set<Int> by lazy {
        (0 until CELLS).filter { cell ->
            val d = values[cell]
            d != 0 && Units.peers[cell].any { values[it] == d }
        }.toSet()
    }

    fun remaining(digit: Int) = SIZE - values.count { it == digit }
}
