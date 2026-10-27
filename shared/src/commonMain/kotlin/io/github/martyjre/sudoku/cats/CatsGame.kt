package io.github.martyjre.sudoku.cats

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable

@Serializable
enum class Mark { EMPTY, X, CAT }

@Serializable
data class CatsGame(
    val puzzle: CatsPuzzle,
    val marks: List<Mark> = List(puzzle.size * puzzle.size) { Mark.EMPTY },
    val elapsedSeconds: Long = 0,
    val hintsUsed: Int = 0,
    val autoX: Boolean = false,
) {
    val size get() = puzzle.size

    val cats: List<Int> get() = marks.indices.filter { marks[it] == Mark.CAT }

    val isSolved: Boolean get() = cats.size == size && cats.all(puzzle::isSolutionCell)

    /** Cats that share a row, column or region with another cat, or touch one. */
    val conflicts: Set<Int> by lazy {
        val cats = cats
        cats.filter { a ->
            cats.any { b ->
                a != b && (a / size == b / size || a % size == b % size ||
                    puzzle.regions[a] == puzzle.regions[b] || touching(a, b, size))
            }
        }.toSet()
    }
}

data class CatsUiState(
    val game: CatsGame,
    val paused: Boolean = false,
    val canUndo: Boolean = false,
)

class CatsController(initial: CatsGame, private val onChange: (CatsGame) -> Unit = {}) {
    private val _state = MutableStateFlow(CatsUiState(initial))
    val state: StateFlow<CatsUiState> = _state.asStateFlow()

    private val history = ArrayDeque<CatsGame>()
    private var dragStart: CatsGame? = null

    private val game get() = _state.value.game
    private val playable get() = !game.isSolved && !_state.value.paused

    /** Tap cycles empty → X → cat → empty. */
    fun tap(cell: Int) {
        if (!playable) return
        val next = when (game.marks[cell]) {
            Mark.EMPTY -> Mark.X
            Mark.X -> Mark.CAT
            Mark.CAT -> Mark.EMPTY
        }
        commit(withMark(game, cell, next))
    }

    /** Dragging across empty cells marks them all with X as a single undo step. */
    fun dragOver(cell: Int) {
        if (!playable || game.marks[cell] != Mark.EMPTY) return
        if (dragStart == null) dragStart = game
        publish(game.copy(marks = game.marks.with(cell, Mark.X)))
    }

    fun endDrag() {
        val start = dragStart ?: return
        history.addLast(start)
        dragStart = null
        publish(game)
    }

    fun toggleAutoX() {
        if (!playable) return
        commit(game.copy(autoX = !game.autoX))
    }

    /** Removes a misplaced cat if there is one, otherwise reveals a missing cat. */
    fun hint() {
        if (!playable) return
        val wrong = game.cats.firstOrNull { !game.puzzle.isSolutionCell(it) }
        val next = if (wrong != null) {
            game.copy(marks = game.marks.with(wrong, Mark.X))
        } else {
            val row = game.puzzle.solution.indices.first { row -> game.marks[row * game.size + game.puzzle.solution[row]] != Mark.CAT }
            withMark(game, row * game.size + game.puzzle.solution[row], Mark.CAT)
        }
        commit(next.copy(hintsUsed = game.hintsUsed + 1))
    }

    fun undo() {
        if (!playable) return
        val previous = history.removeLastOrNull() ?: return
        publish(previous.copy(elapsedSeconds = game.elapsedSeconds, hintsUsed = game.hintsUsed))
    }

    fun togglePause() {
        if (game.isSolved) return
        _state.update { it.copy(paused = !it.paused) }
    }

    fun tick() {
        if (!playable) return
        val next = game.copy(elapsedSeconds = game.elapsedSeconds + 1)
        _state.update { it.copy(game = next) }
        onChange(next)
    }

    private fun withMark(state: CatsGame, cell: Int, mark: Mark): CatsGame {
        val marks = state.marks.toMutableList()
        marks[cell] = mark
        if (mark == Mark.CAT && state.autoX) {
            val n = state.size
            for (other in marks.indices) {
                val blocked = other / n == cell / n || other % n == cell % n ||
                    state.puzzle.regions[other] == state.puzzle.regions[cell] || touching(other, cell, n)
                if (other != cell && blocked && marks[other] == Mark.EMPTY) marks[other] = Mark.X
            }
        }
        return state.copy(marks = marks)
    }

    private fun commit(next: CatsGame) {
        history.addLast(game)
        publish(next)
    }

    private fun publish(next: CatsGame) {
        _state.update { it.copy(game = next, canUndo = history.isNotEmpty()) }
        onChange(next)
    }
}

private fun <T> List<T>.with(index: Int, value: T) = toMutableList().also { it[index] = value }
