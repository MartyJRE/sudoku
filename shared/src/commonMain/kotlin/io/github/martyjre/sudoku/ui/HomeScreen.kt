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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.martyjre.sudoku.engine.Difficulty
import io.github.martyjre.sudoku.game.GameState
import io.github.martyjre.sudoku.storage.Stats

@Composable
fun HomeScreen(
    saved: GameState?,
    stats: Stats,
    onContinue: () -> Unit,
    onNewGame: (Difficulty) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Sudoku", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.SemiBold)
        Text("Unlimited puzzles. No ads.", color = colors.onSurfaceVariant)
        Spacer(Modifier.height(32.dp))

        if (saved != null) {
            Button(onClick = onContinue, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Continue · ${saved.puzzle.difficulty.label} · ${formatTime(saved.elapsedSeconds)}")
            }
            Spacer(Modifier.height(24.dp))
        }

        Text(
            "New game",
            style = MaterialTheme.typography.titleSmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (difficulty in Difficulty.entries) {
                DifficultyRow(difficulty, stats, onClick = { onNewGame(difficulty) })
            }
        }
    }
}

@Composable
private fun DifficultyRow(difficulty: Difficulty, stats: Stats, onClick: () -> Unit) {
    val s = stats[difficulty]
    val summary = buildList {
        add(if (s.won == 1) "1 solved" else "${s.won} solved")
        s.bestSeconds?.let { add("best ${formatTime(it)}") }
        s.averageSeconds?.let { add("avg ${formatTime(it)}") }
    }.joinToString(" · ")

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(difficulty.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
