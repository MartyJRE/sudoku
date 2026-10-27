package io.github.martyjre.sudoku.engine

import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
data class Puzzle(
    val givens: List<Int>,
    val solution: List<Int>,
    val difficulty: Difficulty,
)

class Generator(private val random: Random = Random.Default) {

    /** Keeps generating until a puzzle grades exactly at [difficulty]; falls back to the closest one found. */
    suspend fun generate(difficulty: Difficulty, maxAttempts: Int = 500): Puzzle {
        var fallback: Puzzle? = null
        repeat(maxAttempts) {
            val puzzle = attempt(difficulty)
            if (puzzle.difficulty == difficulty) return puzzle
            if (fallback == null || puzzle.difficulty > fallback.difficulty) fallback = puzzle
            // Lets the UI breathe on single-threaded targets (wasm).
            yield()
        }
        return fallback!!
    }

    /** One pass: random full grid, then remove clue pairs while the puzzle stays unique and within [ceiling]. */
    fun attempt(ceiling: Difficulty): Puzzle {
        val solution = Solver.solve(IntArray(CELLS), random)!!
        val givens = solution.copyOf()
        val minClues = MIN_CLUES.getValue(ceiling)
        var clues = CELLS
        for (cell in (0..CELLS / 2).shuffled(random)) {
            val mirror = CELLS - 1 - cell
            val removed = if (cell == mirror) 1 else 2
            if (clues - removed < minClues) continue
            val saved = givens[cell] to givens[mirror]
            givens[cell] = 0
            givens[mirror] = 0
            if (Solver.hasUniqueSolution(givens) && Grader(givens).grade(ceiling) != null) {
                clues -= removed
            } else {
                givens[cell] = saved.first
                givens[mirror] = saved.second
            }
        }
        val grade = Grader(givens).grade(ceiling) ?: Difficulty.EASY
        return Puzzle(givens.toList(), solution.toList(), grade)
    }

    private companion object {
        val MIN_CLUES = mapOf(
            Difficulty.EASY to 36,
            Difficulty.MEDIUM to 30,
            Difficulty.HARD to 24,
            Difficulty.EXPERT to 17,
        )
    }
}
