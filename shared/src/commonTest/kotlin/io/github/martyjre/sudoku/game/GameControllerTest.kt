package io.github.martyjre.sudoku.game

import com.russhwolf.settings.MapSettings
import io.github.martyjre.sudoku.engine.Difficulty
import io.github.martyjre.sudoku.engine.Generator
import io.github.martyjre.sudoku.storage.Stats
import io.github.martyjre.sudoku.storage.Storage
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameControllerTest {
    private val puzzle = Generator(Random(7)).attempt(Difficulty.EASY)
    private val empty = puzzle.givens.indexOfFirst { it == 0 }
    private val answer = puzzle.solution[empty]

    @Test
    fun notesAreClearedFromPeersWhenADigitIsPlaced() {
        val controller = GameController(GameState(puzzle))
        val peer = puzzle.givens.indices.first { it != empty && it / 9 == empty / 9 && puzzle.givens[it] == 0 }
        controller.toggleCandidateMode()
        controller.select(peer)
        controller.input(answer)
        assertTrue(controller.state.value.game.hasNote(peer, answer))

        controller.toggleCandidateMode()
        controller.select(empty)
        controller.input(answer)
        assertFalse(controller.state.value.game.hasNote(peer, answer))
        assertEquals(answer, controller.state.value.game.values[empty])
    }

    @Test
    fun shiftDigitEntersCandidateInNormalMode() {
        val controller = GameController(GameState(puzzle))
        controller.select(empty)
        controller.input(answer, asCandidate = true)
        val game = controller.state.value.game
        assertEquals(0, game.values[empty])
        assertTrue(game.hasNote(empty, answer))
    }

    @Test
    fun autoCandidatesFillLegalDigitsAndRestoreThemOnErase() {
        val controller = GameController(GameState(puzzle))
        controller.toggleAutoCandidates()
        val filled = controller.state.value.game
        assertTrue(filled.hasNote(empty, answer))
        assertTrue((0 until 81).filter { filled.values[it] != 0 }.all { filled.notes[it] == 0 })

        val peer = (0 until 81).first { it != empty && it / 9 == empty / 9 && filled.hasNote(it, answer) }
        controller.select(empty)
        controller.input(answer)
        assertFalse(controller.state.value.game.hasNote(peer, answer))
        controller.erase()
        assertTrue(controller.state.value.game.hasNote(peer, answer))
        assertTrue(controller.state.value.game.hasNote(empty, answer))

        controller.toggleAutoCandidates()
        assertTrue(controller.state.value.game.notes.all { it == 0 })
    }

    @Test
    fun pauseStopsClockAndInput() {
        val controller = GameController(GameState(puzzle))
        controller.select(empty)
        controller.togglePause()
        controller.tick()
        controller.input(answer)
        assertEquals(0, controller.state.value.game.elapsedSeconds)
        assertEquals(0, controller.state.value.game.values[empty])
        controller.togglePause()
        controller.tick()
        assertEquals(1, controller.state.value.game.elapsedSeconds)
    }

    @Test
    fun undoRestoresPreviousBoardButKeepsTime() {
        val controller = GameController(GameState(puzzle))
        controller.select(empty)
        controller.input(answer)
        controller.tick()
        controller.undo()
        val game = controller.state.value.game
        assertEquals(0, game.values[empty])
        assertEquals(1, game.elapsedSeconds)
        assertFalse(controller.state.value.canUndo)
    }

    @Test
    fun givensCannotBeEdited() {
        val given = puzzle.givens.indexOfFirst { it != 0 }
        val controller = GameController(GameState(puzzle))
        controller.select(given)
        controller.erase()
        assertEquals(puzzle.givens[given], controller.state.value.game.values[given])
    }

    @Test
    fun hintsSolveThePuzzle() {
        val controller = GameController(GameState(puzzle))
        repeat(81) { controller.hint() }
        assertTrue(controller.state.value.game.isSolved)
    }

    @Test
    fun conflictsAreDetected() {
        val controller = GameController(GameState(puzzle))
        val rowGiven = puzzle.givens.indices.first { it / 9 == empty / 9 && puzzle.givens[it] != 0 }
        controller.select(empty)
        controller.input(puzzle.givens[rowGiven])
        assertTrue(controller.state.value.game.conflicts.containsAll(setOf(empty, rowGiven)))
    }

    @Test
    fun gameAndStatsRoundTripThroughStorage() {
        val storage = Storage(MapSettings())
        val game = GameState(puzzle).copy(elapsedSeconds = 42)
        storage.saveGame(game)
        assertEquals(game, storage.loadGame())

        val stats = Stats().started(Difficulty.EASY).won(Difficulty.EASY, 300).won(Difficulty.EASY, 200)
        storage.saveStats(stats)
        val easy = storage.loadStats()[Difficulty.EASY]
        assertEquals(200, easy.bestSeconds)
        assertEquals(250, easy.averageSeconds)
    }
}
