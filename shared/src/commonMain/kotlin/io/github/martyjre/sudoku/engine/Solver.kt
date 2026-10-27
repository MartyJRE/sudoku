package io.github.martyjre.sudoku.engine

import kotlin.random.Random

/** Backtracking solver over row/column/box bitmasks, branching on the most constrained cell. */
class Solver private constructor(givens: IntArray, private val random: Random?) {
    private val cells = givens.copyOf()
    private val rows = IntArray(SIZE)
    private val cols = IntArray(SIZE)
    private val boxes = IntArray(SIZE)
    private var valid = true

    init {
        for (cell in 0 until CELLS) {
            val d = cells[cell]
            if (d == 0) continue
            val b = bit(d)
            if (used(cell) and b != 0) valid = false
            mark(cell, b)
        }
    }

    private fun used(cell: Int) = rows[rowOf(cell)] or cols[colOf(cell)] or boxes[boxOf(cell)]

    private fun mark(cell: Int, b: Int) {
        rows[rowOf(cell)] = rows[rowOf(cell)] or b
        cols[colOf(cell)] = cols[colOf(cell)] or b
        boxes[boxOf(cell)] = boxes[boxOf(cell)] or b
    }

    private fun unmark(cell: Int, b: Int) {
        rows[rowOf(cell)] = rows[rowOf(cell)] and b.inv()
        cols[colOf(cell)] = cols[colOf(cell)] and b.inv()
        boxes[boxOf(cell)] = boxes[boxOf(cell)] and b.inv()
    }

    private var found = 0
    private var first: IntArray? = null

    private fun search(limit: Int) {
        if (found >= limit) return
        var best = -1
        var bestMask = 0
        var bestCount = 10
        for (cell in 0 until CELLS) {
            if (cells[cell] != 0) continue
            val mask = ALL_DIGITS and used(cell).inv()
            val count = mask.countOneBits()
            if (count < bestCount) {
                best = cell
                bestMask = mask
                bestCount = count
                if (count <= 1) break
            }
        }
        if (best == -1) {
            if (found == 0) first = cells.copyOf()
            found++
            return
        }
        val candidates = bestMask.digits().let { if (random != null) it.shuffled(random) else it }
        for (d in candidates) {
            val b = bit(d)
            cells[best] = d
            mark(best, b)
            search(limit)
            unmark(best, b)
            cells[best] = 0
            if (found >= limit) return
        }
    }

    companion object {
        fun solve(givens: IntArray, random: Random? = null): IntArray? {
            val solver = Solver(givens, random)
            if (!solver.valid) return null
            solver.search(1)
            return solver.first
        }

        fun countSolutions(givens: IntArray, limit: Int = 2): Int {
            val solver = Solver(givens, null)
            if (!solver.valid) return 0
            solver.search(limit)
            return solver.found
        }

        fun hasUniqueSolution(givens: IntArray) = countSolutions(givens, 2) == 1
    }
}
