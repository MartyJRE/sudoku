package io.github.martyjre.sudoku.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.martyjre.sudoku.cats.catsBoardSize
import io.github.martyjre.sudoku.engine.Difficulty
import io.github.martyjre.sudoku.storage.GameKind
import io.github.martyjre.sudoku.storage.GameRecord

/** What the Continue button shows for a game in progress. */
data class SavedGameInfo(val difficulty: Difficulty, val elapsedSeconds: Long)

@Composable
fun HomeScreen(
    history: List<GameRecord>,
    savedSudoku: SavedGameInfo?,
    savedCats: SavedGameInfo?,
    onContinue: (GameKind) -> Unit,
    onNewGame: (GameKind, Difficulty) -> Unit,
    onStats: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Puzzles", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        Text("Unlimited. No ads.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        GameSection(GameKind.SUDOKU, history, savedSudoku, onContinue, onNewGame) { it.label }
        Spacer(Modifier.height(28.dp))
        GameSection(GameKind.CATS, history, savedCats, onContinue, onNewGame) {
            val n = catsBoardSize(it)
            "${it.label} · $n×$n"
        }
        Spacer(Modifier.height(28.dp))
        OutlinedButton(onClick = onStats, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Stats") }
    }
}

@Composable
private fun GameSection(
    kind: GameKind,
    history: List<GameRecord>,
    saved: SavedGameInfo?,
    onContinue: (GameKind) -> Unit,
    onNewGame: (GameKind, Difficulty) -> Unit,
    label: (Difficulty) -> String,
) {
    Text(
        kind.label,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
    )
    if (saved != null) {
        Button(onClick = { onContinue(kind) }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Continue · ${saved.difficulty.label} · ${formatTime(saved.elapsedSeconds)}")
        }
        Spacer(Modifier.height(8.dp))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (difficulty in Difficulty.entries) {
            val games = history.filter { it.kind == kind && it.difficulty == difficulty }
            val summary = buildList {
                add("${games.size} solved")
                games.minOfOrNull { it.seconds }?.let { add("best ${formatTime(it)}") }
            }.joinToString(" · ")
            Surface(
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth().clickable { onNewGame(kind, difficulty) },
            ) {
                Row(Modifier.padding(horizontal = 20.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(label(difficulty), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
