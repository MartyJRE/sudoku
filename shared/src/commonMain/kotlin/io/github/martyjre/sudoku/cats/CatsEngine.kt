package io.github.martyjre.sudoku.cats

import io.github.martyjre.sudoku.engine.Difficulty
import kotlinx.coroutines.yield
import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.random.Random

@Serializable
data class CatsPuzzle(
    val size: Int,
    /** Region index per cell, row-major. */
    val regions: List<Int>,
    /** Column of the cat in each row. */
    val solution: List<Int>,
    val difficulty: Difficulty,
) {
    fun isSolutionCell(cell: Int) = solution[cell / size] == cell % size
}

fun catsBoardSize(difficulty: Difficulty) = when (difficulty) {
    Difficulty.EASY -> 6
    Difficulty.MEDIUM -> 7
    Difficulty.HARD -> 8
    Difficulty.EXPERT -> 10
}

fun touching(a: Int, b: Int, size: Int) =
    a != b && abs(a / size - b / size) <= 1 && abs(a % size - b % size) <= 1

/**
 * Places one cat per region, always branching on the region with the fewest legal cells left.
 * That ordering prunes dead ends early enough to count solutions on 10×10 boards in milliseconds.
 */
class CatsSolver(private val size: Int, regions: IntArray) {
    private val regionCells: List<IntArray> = List(size) { r -> regions.indices.filter { regions[it] == r }.toIntArray() }
    private var rows = 0
    private var cols = 0
    private val blocked = IntArray(size * size)
    private var found = 0
    val solutions = mutableListOf<IntArray>()
    val first: IntArray? get() = solutions.firstOrNull()
    private val placement = IntArray(size)

    private fun legal(cell: Int) =
        rows and (1 shl cell / size) == 0 && cols and (1 shl cell % size) == 0 && blocked[cell] == 0

    private fun setCat(cell: Int, delta: Int) {
        val r = cell / size
        val c = cell % size
        for (dr in -1..1) for (dc in -1..1) {
            val nr = r + dr
            val nc = c + dc
            if (nr in 0 until size && nc in 0 until size) blocked[nr * size + nc] += delta
        }
        rows = rows xor (1 shl r)
        cols = cols xor (1 shl c)
    }

    private fun search(placedRegions: Int, limit: Int) {
        if (found >= limit) return
        if (placedRegions == (1 shl size) - 1) {
            solutions += placement.copyOf()
            found++
            return
        }
        var bestRegion = -1
        var bestCount = Int.MAX_VALUE
        for (region in 0 until size) {
            if (placedRegions and (1 shl region) != 0) continue
            val count = regionCells[region].count(::legal)
            if (count == 0) return
            if (count < bestCount) {
                bestCount = count
                bestRegion = region
            }
        }
        for (cell in regionCells[bestRegion]) {
            if (!legal(cell)) continue
            placement[cell / size] = cell % size
            setCat(cell, 1)
            search(placedRegions or (1 shl bestRegion), limit)
            setCat(cell, -1)
            if (found >= limit) return
        }
    }

    fun countSolutions(limit: Int = 2): Int {
        found = 0
        solutions.clear()
        search(0, limit)
        return found
    }
}

class CatsGenerator(private val random: Random = Random.Default) {

    suspend fun generate(difficulty: Difficulty, maxAttempts: Int = 5000): CatsPuzzle {
        val size = catsBoardSize(difficulty)
        repeat(maxAttempts) { attempt ->
            attempt(size, difficulty)?.let { return it }
            // Lets the UI breathe on single-threaded targets (wasm).
            if (attempt % 20 == 19) yield()
        }
        error("No unique $size×$size cats puzzle after $maxAttempts attempts")
    }

    fun attempt(size: Int, difficulty: Difficulty): CatsPuzzle? {
        val cats = randomPlacement(size) ?: return null
        val regions = growRegions(size, cats)
        repeat(MAX_REPAIRS) {
            val solver = CatsSolver(size, regions)
            if (solver.countSolutions(2) == 1) return CatsPuzzle(size, regions.toList(), cats.toList(), difficulty)
            val alternative = solver.solutions.first { !it.contentEquals(cats) }
            if (!breakAlternative(size, regions, cats, alternative)) return null
        }
        return null
    }

    /**
     * Moves one of the alternative solution's cat cells into a neighbouring region, which rules that
     * solution out while keeping ours valid. Much faster than regenerating until a board happens to be unique.
     */
    private fun breakAlternative(size: Int, regions: IntArray, cats: IntArray, alternative: IntArray): Boolean {
        val catCells = cats.mapIndexed { row, col -> row * size + col }.toSet()
        val candidates = alternative.mapIndexed { row, col -> row * size + col }.filter { it !in catCells }.shuffled(random)
        for (cell in candidates) {
            val from = regions[cell]
            val targets = neighbours(cell, size).map { regions[it] }.filter { it != from }.distinct().shuffled(random)
            if (targets.isEmpty() || !staysConnected(size, regions, from, cell)) continue
            regions[cell] = targets.first()
            return true
        }
        return false
    }

    private fun staysConnected(size: Int, regions: IntArray, region: Int, removed: Int): Boolean {
        val cells = regions.indices.filter { regions[it] == region && it != removed }.toSet()
        if (cells.isEmpty()) return false
        val seen = mutableSetOf(cells.first())
        val queue = ArrayDeque(seen)
        while (queue.isNotEmpty()) {
            for (next in neighbours(queue.removeFirst(), size)) if (next in cells && seen.add(next)) queue.add(next)
        }
        return seen.size == cells.size
    }

    private companion object {
        const val MAX_REPAIRS = 300
    }

    /** A random column per row with no two cats in the same column or touching. */
    private fun randomPlacement(size: Int): IntArray? {
        val cols = IntArray(size)
        fun place(row: Int, used: Int): Boolean {
            if (row == size) return true
            for (c in (0 until size).shuffled(random)) {
                if (used and (1 shl c) != 0) continue
                if (row > 0 && abs(cols[row - 1] - c) <= 1) continue
                cols[row] = c
                if (place(row + 1, used or (1 shl c))) return true
            }
            return false
        }
        return if (place(0, 0)) cols else null
    }

    /** Seeds one region at each cat and grows them into random neighbouring cells until the board is covered. */
    private fun growRegions(size: Int, cats: IntArray): IntArray {
        val regions = IntArray(size * size) { -1 }
        cats.forEachIndexed { row, col -> regions[row * size + col] = row }
        var remaining = size * size - size
        while (remaining > 0) {
            val cell = random.nextInt(size * size)
            if (regions[cell] != -1) continue
            val owners = neighbours(cell, size).map { regions[it] }.filter { it != -1 }
            if (owners.isEmpty()) continue
            regions[cell] = owners.random(random)
            remaining--
        }
        return regions
    }
}

fun neighbours(cell: Int, size: Int): List<Int> {
    val r = cell / size
    val c = cell % size
    return buildList {
        if (r > 0) add(cell - size)
        if (r < size - 1) add(cell + size)
        if (c > 0) add(cell - 1)
        if (c < size - 1) add(cell + 1)
    }
}
