package io.github.martyjre.sudoku.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.martyjre.sudoku.game.GameController
import io.github.martyjre.sudoku.game.UiState
import kotlinx.coroutines.delay

@Composable
fun GameScreen(
    controller: GameController,
    onBack: () -> Unit,
    onNewGame: () -> Unit,
) {
    val state by controller.state.collectAsStateWithLifecycle()
    val game = state.game
    val haptics = LocalHapticFeedback.current
    val focus = remember { FocusRequester() }

    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val running = lifecycleState.isAtLeast(Lifecycle.State.RESUMED) && !game.isSolved && !state.paused
    LaunchedEffect(controller, running) {
        while (running) {
            delay(1000)
            controller.tick()
        }
    }
    LaunchedEffect(Unit) { focus.requestFocus() }

    val input = { digit: Int, asCandidate: Boolean ->
        controller.input(digit, asCandidate)
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .onPreviewKeyEvent { handleKey(it, controller, input) }
    ) {
        val wide = maxWidth >= 720.dp
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Toolbar(state, controller, onBack)
            Spacer(Modifier.height(8.dp))
            if (wide) {
                Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                    BoardArea(state, controller, Modifier.weight(1f))
                    Controls(state, controller, input, Modifier.width(280.dp))
                }
            } else {
                BoardArea(state, controller, Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Controls(state, controller, input, Modifier.fillMaxWidth())
            }
        }

        if (game.isSolved) {
            SolvedOverlay(
                seconds = game.elapsedSeconds,
                hints = game.hintsUsed,
                difficulty = game.puzzle.difficulty.label,
                onNewGame = onNewGame,
                onBack = onBack,
            )
        }
    }
}

@Composable
private fun Toolbar(state: UiState, controller: GameController, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth().height(56.dp), verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onBack) { Icon(AppIcons.Back, contentDescription = "Back") }
        Text(state.game.puzzle.difficulty.label, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(16.dp))
        Text(
            formatTime(state.game.elapsedSeconds),
            style = MaterialTheme.typography.titleMedium.copy(fontFeatureSettings = "tnum"),
            color = colors.onSurfaceVariant,
        )
        IconButton(onClick = controller::togglePause, enabled = !state.game.isSolved) {
            Icon(
                if (state.paused) AppIcons.Play else AppIcons.Pause,
                contentDescription = if (state.paused) "Resume" else "Pause",
                tint = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        TextButton(onClick = controller::undo, enabled = state.canUndo && !state.paused) { Text("Undo") }
        TextButton(onClick = controller::hint, enabled = !state.paused) { Text("Hint") }
    }
}

@Composable
private fun BoardArea(state: UiState, controller: GameController, modifier: Modifier) {
    Box(modifier.aspectRatio(1f)) {
        if (state.paused) {
            Box(
                Modifier.fillMaxSize().clip(RoundedCornerShape(4.dp)).background(LocalBoardColors.current.peer),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Paused", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = controller::togglePause) { Text("Resume") }
                }
            }
        } else {
            Board(state, onSelect = controller::select, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun Controls(
    state: UiState,
    controller: GameController,
    input: (Int, Boolean) -> Unit,
    modifier: Modifier,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        ModeSwitch(state.candidateMode, controller::setCandidateMode)
        NumberPad(state, onDigit = { input(it, state.candidateMode) })
        OutlinedButton(
            onClick = controller::erase,
            enabled = !state.paused,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text("Erase") }
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable(enabled = !state.paused, onClick = controller::toggleAutoCandidates)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Auto Candidate Mode", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Switch(
                checked = state.game.autoCandidates,
                onCheckedChange = { controller.toggleAutoCandidates() },
                enabled = !state.paused,
            )
        }
    }
}

@Composable
private fun ModeSwitch(candidate: Boolean, onChange: (Boolean) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(22.dp))
            .border(1.dp, colors.outlineVariant, RoundedCornerShape(22.dp))
            .padding(4.dp),
    ) {
        for ((label, value) in listOf("Normal" to false, "Candidate" to true)) {
            val active = candidate == value
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (active) colors.primary else Color.Transparent)
                    .clickable { onChange(value) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) colors.onPrimary else colors.onSurfaceVariant,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}

@Composable
private fun NumberPad(state: UiState, onDigit: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 3) {
                    val digit = row * 3 + col + 1
                    val done = state.game.remaining(digit) <= 0
                    // Candidate keys show the digit where it sits inside a cell's pencil-mark grid.
                    val alignment = if (state.candidateMode) {
                        BiasAlignment(horizontalBias = col - 1f, verticalBias = row - 1f)
                    } else {
                        Alignment.Center
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(colors.surfaceVariant)
                            .clickable(enabled = !done && !state.paused) { onDigit(digit) }
                            .alpha(if (done) 0.3f else 1f)
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                            .semantics { contentDescription = digit.toString() },
                        contentAlignment = alignment,
                    ) {
                        Text(
                            digit.toString(),
                            fontSize = if (state.candidateMode) 15.sp else 28.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (state.candidateMode) colors.onSurfaceVariant else colors.onSurface,
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SolvedOverlay(seconds: Long, hints: Int, difficulty: String, onNewGame: () -> Unit, onBack: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)).clickable(enabled = false) {},
        contentAlignment = Alignment.Center,
    ) {
        Surface(shape = RoundedCornerShape(24.dp), tonalElevation = 6.dp, modifier = Modifier.padding(32.dp)) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Solved!", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "$difficulty in ${formatTime(seconds)}" + if (hints > 0) " · $hints hint${if (hints == 1) "" else "s"}" else "",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = onNewGame, modifier = Modifier.fillMaxWidth()) { Text("New $difficulty game") }
                OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Home") }
            }
        }
    }
}

private val digitKeys = mapOf(
    Key.One to 1, Key.Two to 2, Key.Three to 3, Key.Four to 4, Key.Five to 5,
    Key.Six to 6, Key.Seven to 7, Key.Eight to 8, Key.Nine to 9,
    Key.NumPad1 to 1, Key.NumPad2 to 2, Key.NumPad3 to 3, Key.NumPad4 to 4, Key.NumPad5 to 5,
    Key.NumPad6 to 6, Key.NumPad7 to 7, Key.NumPad8 to 8, Key.NumPad9 to 9,
)

private fun handleKey(event: KeyEvent, controller: GameController, input: (Int, Boolean) -> Unit): Boolean {
    if (event.type != KeyEventType.KeyDown) return false
    digitKeys[event.key]?.let {
        input(it, event.isShiftPressed || controller.state.value.candidateMode)
        return true
    }
    when (event.key) {
        Key.DirectionUp -> controller.moveSelection(-1, 0)
        Key.DirectionDown -> controller.moveSelection(1, 0)
        Key.DirectionLeft -> controller.moveSelection(0, -1)
        Key.DirectionRight -> controller.moveSelection(0, 1)
        Key.Backspace, Key.Delete, Key.Zero -> controller.erase()
        Key.Spacebar -> controller.toggleCandidateMode()
        Key.Z -> if (event.isCtrlPressed || event.isMetaPressed) controller.undo() else return false
        else -> return false
    }
    return true
}

fun formatTime(seconds: Long): String {
    val h = seconds / 3600
    val m = seconds % 3600 / 60
    val s = seconds % 60
    val mm = if (h > 0) m.toString().padStart(2, '0') else m.toString()
    return (if (h > 0) "$h:" else "") + "$mm:${s.toString().padStart(2, '0')}"
}
