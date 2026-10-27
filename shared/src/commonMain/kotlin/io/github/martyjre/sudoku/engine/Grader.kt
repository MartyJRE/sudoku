package io.github.martyjre.sudoku.engine

import kotlinx.serialization.Serializable

@Serializable
enum class Difficulty(val label: String) {
    EASY("Easy"),
    MEDIUM("Medium"),
    HARD("Hard"),
    EXPERT("Expert"),
}

enum class Technique(val difficulty: Difficulty) {
    NAKED_SINGLE(Difficulty.EASY),
    HIDDEN_SINGLE(Difficulty.EASY),
    LOCKED_CANDIDATES(Difficulty.MEDIUM),
    NAKED_PAIR(Difficulty.MEDIUM),
    HIDDEN_PAIR(Difficulty.MEDIUM),
    NAKED_TRIPLE(Difficulty.HARD),
    HIDDEN_TRIPLE(Difficulty.HARD),
    X_WING(Difficulty.HARD),
    NAKED_QUAD(Difficulty.EXPERT),
    HIDDEN_QUAD(Difficulty.EXPERT),
    SWORDFISH(Difficulty.EXPERT),
    XY_WING(Difficulty.EXPERT),
}

/**
 * Solves the way a person would, trying the easiest technique first after every step.
 * The hardest technique a puzzle needs sets its difficulty; puzzles that need guessing get none.
 */
class Grader(givens: IntArray) {
    private val values = givens.copyOf()
    private val candidates = IntArray(CELLS)
    val used = mutableSetOf<Technique>()

    init {
        for (cell in 0 until CELLS) {
            if (values[cell] != 0) continue
            var mask = ALL_DIGITS
            for (peer in Units.peers[cell]) if (values[peer] != 0) mask = mask and bit(values[peer]).inv()
            candidates[cell] = mask
        }
    }

    private val steps: List<Pair<Technique, () -> Boolean>> = listOf(
        Technique.NAKED_SINGLE to ::nakedSingle,
        Technique.HIDDEN_SINGLE to ::hiddenSingle,
        Technique.LOCKED_CANDIDATES to ::lockedCandidates,
        Technique.NAKED_PAIR to { nakedSubset(2) },
        Technique.HIDDEN_PAIR to { hiddenSubset(2) },
        Technique.NAKED_TRIPLE to { nakedSubset(3) },
        Technique.HIDDEN_TRIPLE to { hiddenSubset(3) },
        Technique.X_WING to { fish(2) },
        Technique.NAKED_QUAD to { nakedSubset(4) },
        Technique.HIDDEN_QUAD to { hiddenSubset(4) },
        Technique.SWORDFISH to { fish(3) },
        Technique.XY_WING to ::xyWing,
    )

    /** Returns the puzzle's difficulty, or null when these techniques can't finish it. */
    fun grade(ceiling: Difficulty = Difficulty.EXPERT): Difficulty? {
        while (values.any { it == 0 }) {
            val step = steps.firstOrNull { (technique, apply) ->
                technique.difficulty <= ceiling && apply()
            } ?: return null
            used += step.first
        }
        return used.maxOfOrNull { it.difficulty } ?: Difficulty.EASY
    }

    private fun place(cell: Int, digit: Int) {
        values[cell] = digit
        candidates[cell] = 0
        val b = bit(digit).inv()
        for (peer in Units.peers[cell]) candidates[peer] = candidates[peer] and b
    }

    private fun eliminate(cell: Int, mask: Int): Boolean {
        if (candidates[cell] and mask == 0) return false
        candidates[cell] = candidates[cell] and mask.inv()
        return true
    }

    private fun nakedSingle(): Boolean {
        for (cell in 0 until CELLS) {
            if (values[cell] == 0 && candidates[cell].countOneBits() == 1) {
                place(cell, candidates[cell].countTrailingZeroBits() + 1)
                return true
            }
        }
        return false
    }

    private fun hiddenSingle(): Boolean {
        for (unit in Units.all) {
            for (d in 1..SIZE) {
                val b = bit(d)
                var only = -1
                var count = 0
                for (cell in unit) if (candidates[cell] and b != 0) { only = cell; count++ }
                if (count == 1) {
                    place(only, d)
                    return true
                }
            }
        }
        return false
    }

    private fun lockedCandidates(): Boolean {
        var progress = false
        for (d in 1..SIZE) {
            val b = bit(d)
            // Pointing: a box's candidates for d all lie on one line.
            for (box in Units.boxes) {
                val cells = box.filter { candidates[it] and b != 0 }
                if (cells.size < 2) continue
                val line = when {
                    cells.all { rowOf(it) == rowOf(cells[0]) } -> Units.rows[rowOf(cells[0])]
                    cells.all { colOf(it) == colOf(cells[0]) } -> Units.cols[colOf(cells[0])]
                    else -> continue
                }
                for (cell in line) if (cell !in box) progress = eliminate(cell, b) || progress
            }
            // Claiming: a line's candidates for d all lie in one box.
            for (line in Units.rows + Units.cols) {
                val cells = line.filter { candidates[it] and b != 0 }
                if (cells.size < 2 || cells.any { boxOf(it) != boxOf(cells[0]) }) continue
                for (cell in Units.boxes[boxOf(cells[0])]) if (cell !in line) progress = eliminate(cell, b) || progress
            }
            if (progress) return true
        }
        return false
    }

    private fun nakedSubset(size: Int): Boolean {
        for (unit in Units.all) {
            val open = unit.filter { values[it] == 0 }
            if (open.size <= size) continue
            for (combo in combinations(open, size)) {
                val union = combo.fold(0) { acc, cell -> acc or candidates[cell] }
                if (union.countOneBits() != size) continue
                var progress = false
                for (cell in open) if (cell !in combo) progress = eliminate(cell, union) || progress
                if (progress) return true
            }
        }
        return false
    }

    private fun hiddenSubset(size: Int): Boolean {
        for (unit in Units.all) {
            val open = unit.filter { values[it] == 0 }
            if (open.size <= size) continue
            val digits = (1..SIZE).filter { d -> open.any { candidates[it] and bit(d) != 0 } }
            for (combo in combinations(digits, size)) {
                val mask = combo.fold(0) { acc, d -> acc or bit(d) }
                val cells = open.filter { candidates[it] and mask != 0 }
                if (cells.size != size) continue
                var progress = false
                for (cell in cells) progress = eliminate(cell, mask.inv() and ALL_DIGITS) || progress
                if (progress) return true
            }
        }
        return false
    }

    private fun fish(size: Int): Boolean {
        for (d in 1..SIZE) {
            val b = bit(d)
            for ((base, cover) in listOf(Units.rows to Units.cols, Units.cols to Units.rows)) {
                // For each base line, the cover-line indexes where d can still go.
                val positions = base.map { line ->
                    line.indices.filter { candidates[line[it]] and b != 0 }.fold(0) { acc, i -> acc or (1 shl i) }
                }
                val eligible = positions.indices.filter { positions[it].countOneBits() in 2..size }
                for (combo in combinations(eligible, size)) {
                    val union = combo.fold(0) { acc, i -> acc or positions[i] }
                    if (union.countOneBits() != size) continue
                    var progress = false
                    for (c in 0 until SIZE) {
                        if (union and (1 shl c) == 0) continue
                        for ((i, cell) in cover[c].withIndex()) {
                            if (i !in combo) progress = eliminate(cell, b) || progress
                        }
                    }
                    if (progress) return true
                }
            }
        }
        return false
    }

    private fun xyWing(): Boolean {
        val pairs = (0 until CELLS).filter { candidates[it].countOneBits() == 2 }
        for (pivot in pairs) {
            val pivotMask = candidates[pivot]
            val wings = pairs.filter { it != pivot && Units.sees(it, pivot) }
            for (a in wings) {
                val shared = candidates[a] and pivotMask
                if (shared.countOneBits() != 1) continue
                val z = candidates[a] and pivotMask.inv()
                for (c in wings) {
                    if (c == a || candidates[c] != ((pivotMask and shared.inv()) or z)) continue
                    var progress = false
                    for (cell in 0 until CELLS) {
                        if (cell != a && cell != c && cell != pivot && Units.sees(cell, a) && Units.sees(cell, c)) {
                            progress = eliminate(cell, z) || progress
                        }
                    }
                    if (progress) return true
                }
            }
        }
        return false
    }
}

internal fun <T> combinations(items: List<T>, size: Int): Sequence<List<T>> = sequence {
    if (size == 0) {
        yield(emptyList())
        return@sequence
    }
    for (i in 0..items.size - size) {
        for (rest in combinations(items.subList(i + 1, items.size), size - 1)) yield(listOf(items[i]) + rest)
    }
}
