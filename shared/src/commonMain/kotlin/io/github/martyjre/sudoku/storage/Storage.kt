package io.github.martyjre.sudoku.storage

import com.russhwolf.settings.Settings
import io.github.martyjre.sudoku.cats.CatsGame
import io.github.martyjre.sudoku.engine.Difficulty
import io.github.martyjre.sudoku.game.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
enum class GameKind(val label: String) {
    SUDOKU("Sudoku"),
    CATS("Cats"),
}

@Serializable
data class GameRecord(
    val kind: GameKind,
    val difficulty: Difficulty,
    val seconds: Long,
    val hints: Int,
    val finishedAtMillis: Long,
)

class Storage(private val settings: Settings = Settings()) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadSudoku(): GameState? = read(SUDOKU_KEY)

    fun saveSudoku(game: GameState?) = write(SUDOKU_KEY, game)

    fun loadCats(): CatsGame? = read(CATS_KEY)

    fun saveCats(game: CatsGame?) = write(CATS_KEY, game)

    fun loadHistory(): List<GameRecord> = read(HISTORY_KEY) ?: emptyList()

    fun addRecord(record: GameRecord): List<GameRecord> =
        (loadHistory() + record).also { write(HISTORY_KEY, it) }

    // A save from an incompatible older version is dropped rather than crashing the app.
    private inline fun <reified T> read(key: String): T? =
        settings.getStringOrNull(key)?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }

    private inline fun <reified T> write(key: String, value: T?) {
        if (value == null) settings.remove(key) else settings.putString(key, json.encodeToString(value))
    }

    private companion object {
        const val SUDOKU_KEY = "game.v1"
        const val CATS_KEY = "cats.v1"
        const val HISTORY_KEY = "history.v1"
    }
}
