package io.github.martyjre.sudoku

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.martyjre.sudoku.cats.CatsController
import io.github.martyjre.sudoku.cats.CatsGame
import io.github.martyjre.sudoku.cats.CatsGenerator
import io.github.martyjre.sudoku.engine.Difficulty
import io.github.martyjre.sudoku.engine.Generator
import io.github.martyjre.sudoku.game.GameController
import io.github.martyjre.sudoku.game.GameState
import io.github.martyjre.sudoku.storage.GameKind
import io.github.martyjre.sudoku.storage.GameRecord
import io.github.martyjre.sudoku.storage.Storage
import io.github.martyjre.sudoku.ui.CatsScreen
import io.github.martyjre.sudoku.ui.GameScreen
import io.github.martyjre.sudoku.ui.HomeScreen
import io.github.martyjre.sudoku.ui.SavedGameInfo
import io.github.martyjre.sudoku.ui.StatsScreen
import io.github.martyjre.sudoku.ui.SudokuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock

private sealed interface Screen {
    data object Home : Screen
    data object Stats : Screen
    data object Generating : Screen
    data class Sudoku(val controller: GameController) : Screen
    data class Cats(val controller: CatsController) : Screen
}

@Composable
fun App() {
    val storage = remember { Storage() }
    var history by remember { mutableStateOf(storage.loadHistory()) }
    var sudoku by remember { mutableStateOf(storage.loadSudoku()?.takeUnless { it.isSolved }) }
    var cats by remember { mutableStateOf(storage.loadCats()?.takeUnless { it.isSolved }) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val scope = rememberCoroutineScope()

    fun record(kind: GameKind, difficulty: Difficulty, seconds: Long, hints: Int) {
        history = storage.addRecord(
            GameRecord(kind, difficulty, seconds, hints, Clock.System.now().toEpochMilliseconds())
        )
    }

    fun playSudoku(game: GameState) {
        screen = Screen.Sudoku(GameController(game) { next ->
            if (next.isSolved) {
                record(GameKind.SUDOKU, next.puzzle.difficulty, next.elapsedSeconds, next.hintsUsed)
                storage.saveSudoku(null)
                sudoku = null
            } else {
                storage.saveSudoku(next)
                sudoku = next
            }
        })
    }

    fun playCats(game: CatsGame) {
        screen = Screen.Cats(CatsController(game) { next ->
            if (next.isSolved) {
                record(GameKind.CATS, next.puzzle.difficulty, next.elapsedSeconds, next.hintsUsed)
                storage.saveCats(null)
                cats = null
            } else {
                storage.saveCats(next)
                cats = next
            }
        })
    }

    fun newGame(kind: GameKind, difficulty: Difficulty) {
        screen = Screen.Generating
        scope.launch {
            when (kind) {
                GameKind.SUDOKU -> {
                    val puzzle = withContext(Dispatchers.Default) { Generator().generate(difficulty) }
                    playSudoku(GameState(puzzle).also { storage.saveSudoku(it); sudoku = it })
                }
                GameKind.CATS -> {
                    val puzzle = withContext(Dispatchers.Default) { CatsGenerator().generate(difficulty) }
                    playCats(CatsGame(puzzle).also { storage.saveCats(it); cats = it })
                }
            }
        }
    }

    SudokuTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                when (val current = screen) {
                    Screen.Home -> Box(Modifier.widthIn(max = 520.dp)) {
                        HomeScreen(
                            history = history,
                            savedSudoku = sudoku?.let { SavedGameInfo(it.puzzle.difficulty, it.elapsedSeconds) },
                            savedCats = cats?.let { SavedGameInfo(it.puzzle.difficulty, it.elapsedSeconds) },
                            onContinue = { kind ->
                                when (kind) {
                                    GameKind.SUDOKU -> sudoku?.let(::playSudoku)
                                    GameKind.CATS -> cats?.let(::playCats)
                                }
                            },
                            onNewGame = ::newGame,
                            onStats = { screen = Screen.Stats },
                        )
                    }
                    Screen.Stats -> Box(Modifier.widthIn(max = 720.dp)) {
                        StatsScreen(history, onBack = { screen = Screen.Home })
                    }
                    Screen.Generating -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is Screen.Sudoku -> Box(Modifier.widthIn(max = 960.dp)) {
                        GameScreen(
                            controller = current.controller,
                            onBack = { screen = Screen.Home },
                            onNewGame = { newGame(GameKind.SUDOKU, current.controller.state.value.game.puzzle.difficulty) },
                        )
                    }
                    is Screen.Cats -> Box(Modifier.widthIn(max = 720.dp)) {
                        CatsScreen(
                            controller = current.controller,
                            onBack = { screen = Screen.Home },
                            onNewGame = { newGame(GameKind.CATS, current.controller.state.value.game.puzzle.difficulty) },
                        )
                    }
                }
            }
        }
    }
}
