package io.github.martyjre.sudoku.game

import io.github.martyjre.sudoku.engine.ALL_DIGITS
import io.github.martyjre.sudoku.engine.CELLS
import io.github.martyjre.sudoku.engine.Units
import io.github.martyjre.sudoku.engine.bit
import io.github.martyjre.sudoku.engine.colOf
import io.github.martyjre.sudoku.engine.rowOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class UiState(
    val game: GameState,
    val selected: Int? = null,
    val candidateMode: Boolean = false,
    val paused: Boolean = false,
    val canUndo: Boolean = false,
)

class GameController(initial: GameState, private val onChange: (GameState) -> Unit = {}) {
    private val _state = MutableStateFlow(UiState(initial))
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val history = ArrayDeque<GameState>()

    private val game get() = _state.value.game
    private val playable get() = !game.isSolved && !_state.value.paused

    fun select(cell: Int) = _state.update { it.copy(selected = cell) }

    fun moveSelection(dRow: Int, dCol: Int) {
        val current = _state.value.selected ?: 0
        val row = (rowOf(current) + dRow).mod(9)
        val col = (colOf(current) + dCol).mod(9)
        select(row * 9 + col)
    }

    fun setCandidateMode(on: Boolean) = _state.update { it.copy(candidateMode = on) }

    fun toggleCandidateMode() = setCandidateMode(!_state.value.candidateMode)

    fun togglePause() {
        if (game.isSolved) return
        _state.update { it.copy(paused = !it.paused) }
    }

    /** Turning auto candidates on fills every empty cell with its legal digits; turning it off clears them. */
    fun toggleAutoCandidates() {
        if (!playable) return
        val on = !game.autoCandidates
        val notes = if (on) List(CELLS) { legalCandidates(game.values, it) } else List(CELLS) { 0 }
        commit(game.copy(autoCandidates = on, notes = notes))
    }

    fun input(digit: Int, asCandidate: Boolean = _state.value.candidateMode) {
        val cell = editableSelection() ?: return
        if (asCandidate) toggleCandidate(cell, digit) else place(cell, digit)
    }

    fun erase() {
        val cell = editableSelection() ?: return
        when {
            game.values[cell] != 0 -> commit(cleared(game, cell))
            game.notes[cell] != 0 -> commit(game.copy(notes = game.notes.with(cell, 0)))
        }
    }

    /** Reveals the selected cell, or the first unsolved one when nothing editable is selected. */
    fun hint() {
        if (!playable) return
        val cell = editableSelection()?.takeIf { game.values[it] != game.puzzle.solution[it] }
            ?: (0 until CELLS).firstOrNull { game.values[it] != game.puzzle.solution[it] }
            ?: return
        _state.update { it.copy(selected = cell) }
        place(cell, game.puzzle.solution[cell], hint = true)
    }

    fun undo() {
        if (!playable) return
        val previous = history.removeLastOrNull() ?: return
        publish(previous.copy(elapsedSeconds = game.elapsedSeconds, hintsUsed = game.hintsUsed))
    }

    fun tick() {
        if (!playable) return
        val next = game.copy(elapsedSeconds = game.elapsedSeconds + 1)
        _state.update { it.copy(game = next) }
        onChange(next)
    }

    private fun place(cell: Int, digit: Int, hint: Boolean = false) {
        if (game.values[cell] == digit && !hint) {
            commit(cleared(game, cell))
            return
        }
        val base = if (game.values[cell] != 0) cleared(game, cell) else game
        val clearBit = bit(digit).inv()
        val notes = base.notes.mapIndexed { i, mask ->
            when {
                i == cell -> 0
                i in Units.peers[cell] -> mask and clearBit
                else -> mask
            }
        }
        commit(
            base.copy(
                values = base.values.with(cell, digit),
                notes = notes,
                hintsUsed = game.hintsUsed + if (hint) 1 else 0,
            )
        )
    }

    private fun toggleCandidate(cell: Int, digit: Int) {
        if (game.values[cell] != 0) return
        commit(game.copy(notes = game.notes.with(cell, game.notes[cell] xor bit(digit))))
    }

    /** Removes a cell's value; with auto candidates on, the freed digit goes back where it is legal again. */
    private fun cleared(state: GameState, cell: Int): GameState {
        val removed = state.values[cell]
        val values = state.values.with(cell, 0)
        if (!state.autoCandidates) return state.copy(values = values)
        val notes = state.notes.toMutableList()
        notes[cell] = legalCandidates(values, cell)
        for (peer in Units.peers[cell]) {
            if (values[peer] == 0 && legalCandidates(values, peer) and bit(removed) != 0) {
                notes[peer] = notes[peer] or bit(removed)
            }
        }
        return state.copy(values = values, notes = notes)
    }

    private fun editableSelection(): Int? =
        _state.value.selected?.takeIf { !game.isGiven(it) && playable }

    private fun commit(next: GameState) {
        history.addLast(game)
        publish(next)
    }

    private fun publish(next: GameState) {
        _state.update { it.copy(game = next, canUndo = history.isNotEmpty()) }
        onChange(next)
    }
}

private fun legalCandidates(values: List<Int>, cell: Int): Int {
    if (values[cell] != 0) return 0
    var mask = ALL_DIGITS
    for (peer in Units.peers[cell]) if (values[peer] != 0) mask = mask and bit(values[peer]).inv()
    return mask
}

private fun List<Int>.with(index: Int, value: Int) = toMutableList().also { it[index] = value }
