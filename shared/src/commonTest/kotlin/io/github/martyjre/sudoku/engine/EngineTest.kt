package io.github.martyjre.sudoku.engine

import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EngineTest {
    // Arto Inkala's "world's hardest sudoku": needs guessing, so the grader must give up.
    private val inkala = "8..........36......7..9.2...5...7.......457.....1...3...1....68..85...1..9....4.."
    private val singlesOnly = "..3.2.6..9..3.5..1..18.64....81.29..7.......8..67.82....26.95..8..2.3..9..5.1.3.."
    private val xWing = "1.....569492.561.8.561.924...964.8.1.64.1....218.356.4.4.5...169.5.614.2621.....5"

    @Test
    fun solvesHardestKnownPuzzle() {
        val solution = assertNotNull(Solver.solve(parseGrid(inkala)))
        assertTrue(isValidSolution(solution))
        assertTrue(Solver.hasUniqueSolution(parseGrid(inkala)))
    }

    @Test
    fun detectsMultipleAndNoSolutions() {
        assertEquals(2, Solver.countSolutions(IntArray(CELLS)))
        val clash = IntArray(CELLS).also { it[0] = 5; it[1] = 5 }
        assertEquals(0, Solver.countSolutions(clash))
    }

    @Test
    fun gradesReferencePuzzles() {
        assertEquals(Difficulty.EASY, Grader(parseGrid(singlesOnly)).grade())
        val xWingGrader = Grader(parseGrid(xWing))
        assertEquals(Difficulty.HARD, xWingGrader.grade())
        assertTrue(Technique.X_WING in xWingGrader.used)
        assertNull(Grader(parseGrid(inkala)).grade())
    }

    @Test
    fun generatedPuzzlesAreUniqueAndMatchTheirGrade() = runTest {
        val generator = Generator(Random(42))
        for (difficulty in Difficulty.entries) {
            repeat(3) {
                val puzzle = generator.generate(difficulty)
                val givens = puzzle.givens.toIntArray()
                assertTrue(Solver.hasUniqueSolution(givens), "not unique: ${givens.toGridString()}")
                assertEquals(puzzle.solution, Solver.solve(givens)!!.toList())
                assertEquals(puzzle.difficulty, Grader(givens).grade())
                assertEquals(difficulty, puzzle.difficulty, "wanted $difficulty: ${givens.toGridString()}")
            }
        }
    }

    private fun isValidSolution(grid: IntArray) =
        Units.all.all { unit -> unit.map { grid[it] }.toSet() == (1..SIZE).toSet() }
}
