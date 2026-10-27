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
import io.github.martyjre.sudoku.engine.Difficulty
import io.github.martyjre.sudoku.engine.Generator
import io.github.martyjre.sudoku.game.GameController
import io.github.martyjre.sudoku.game.GameState
import io.github.martyjre.sudoku.storage.Storage
import io.github.martyjre.sudoku.ui.GameScreen
import io.github.martyjre.sudoku.ui.HomeScreen
import io.github.martyjre.sudoku.ui.SudokuTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private sealed interface Screen {
    data object Home : Screen
    data object Generating : Screen
    data class Playing(val controller: GameController) : Screen
}

@Composable
fun App() {
    val storage = remember { Storage() }
    var stats by remember { mutableStateOf(storage.loadStats()) }
    var saved by remember { mutableStateOf(storage.loadGame()?.takeUnless { it.isSolved }) }
    var screen by remember { mutableStateOf<Screen>(Screen.Home) }
    val scope = rememberCoroutineScope()

    fun play(game: GameState) {
        val controller = GameController(game) { next ->
            if (next.isSolved) {
                stats = stats.won(next.puzzle.difficulty, next.elapsedSeconds).also(storage::saveStats)
                storage.saveGame(null)
                saved = null
            } else {
                storage.saveGame(next)
                saved = next
            }
        }
        screen = Screen.Playing(controller)
    }

    fun newGame(difficulty: Difficulty) {
        screen = Screen.Generating
        scope.launch {
            val puzzle = withContext(Dispatchers.Default) { Generator().generate(difficulty) }
            stats = stats.started(difficulty).also(storage::saveStats)
            val game = GameState(puzzle)
            storage.saveGame(game)
            saved = game
            play(game)
        }
    }

    SudokuTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
                Box(Modifier.widthIn(max = 960.dp)) {
                    when (val current = screen) {
                        Screen.Home -> Box(Modifier.widthIn(max = 520.dp)) {
                            HomeScreen(
                                saved = saved,
                                stats = stats,
                                onContinue = { saved?.let(::play) },
                                onNewGame = ::newGame,
                            )
                        }
                        Screen.Generating -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        is Screen.Playing -> GameScreen(
                            controller = current.controller,
                            onBack = { screen = Screen.Home },
                            onNewGame = { newGame(current.controller.state.value.game.puzzle.difficulty) },
                        )
                    }
                }
            }
        }
    }
}
