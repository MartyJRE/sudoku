package io.github.martyjre.sudoku.cats

import io.github.martyjre.sudoku.engine.Difficulty
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.measureTime

class CatsEngineTest {
    @Test
    fun generatedPuzzlesHaveOneValidSolution() = runTest {
        val generator = CatsGenerator(Random(3))
        for (difficulty in Difficulty.entries) {
            repeat(3) {
                val elapsed = measureTime {
                    val puzzle = generator.generate(difficulty)
                    val n = puzzle.size
                    assertEquals(catsBoardSize(difficulty), n)
                    assertEquals(n, puzzle.regions.toSet().size)
                    assertEquals(n, puzzle.solution.toSet().size, "one cat per column")
                    val cells = puzzle.solution.mapIndexed { row, col -> row * n + col }
                    assertEquals(n, cells.map { puzzle.regions[it] }.toSet().size, "one cat per region")
                    assertTrue(cells.none { a -> cells.any { b -> touching(a, b, n) } }, "no touching cats")

                    val solver = CatsSolver(n, puzzle.regions.toIntArray())
                    assertEquals(1, solver.countSolutions())
                    assertEquals(puzzle.solution, solver.first!!.toList())
                }
                println("$difficulty generated in $elapsed")
            }
        }
    }

    @Test
    fun regionsAreConnected() = runTest {
        val puzzle = CatsGenerator(Random(9)).generate(Difficulty.EXPERT)
        val n = puzzle.size
        for (region in 0 until n) {
            val cells = puzzle.regions.indices.filter { puzzle.regions[it] == region }.toSet()
            val seen = mutableSetOf(cells.first())
            val queue = ArrayDeque(seen)
            while (queue.isNotEmpty()) {
                for (next in neighbours(queue.removeFirst(), n)) if (next in cells && seen.add(next)) queue.add(next)
            }
            assertEquals(cells, seen, "region $region is split")
        }
    }
}
