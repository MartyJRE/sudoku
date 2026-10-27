package io.github.martyjre.sudoku.storage

import com.russhwolf.settings.Settings
import io.github.martyjre.sudoku.engine.Difficulty
import io.github.martyjre.sudoku.game.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class DifficultyStats(
    val started: Int = 0,
    val won: Int = 0,
    val bestSeconds: Long? = null,
    val totalWinSeconds: Long = 0,
) {
    val averageSeconds: Long? get() = if (won == 0) null else totalWinSeconds / won
}

@Serializable
data class Stats(val byDifficulty: Map<Difficulty, DifficultyStats> = emptyMap()) {
    operator fun get(difficulty: Difficulty) = byDifficulty[difficulty] ?: DifficultyStats()

    fun started(difficulty: Difficulty) = update(difficulty) { it.copy(started = it.started + 1) }

    fun won(difficulty: Difficulty, seconds: Long) = update(difficulty) {
        it.copy(
            won = it.won + 1,
            bestSeconds = minOf(it.bestSeconds ?: seconds, seconds),
            totalWinSeconds = it.totalWinSeconds + seconds,
        )
    }

    private fun update(difficulty: Difficulty, change: (DifficultyStats) -> DifficultyStats) =
        Stats(byDifficulty + (difficulty to change(this[difficulty])))
}

class Storage(private val settings: Settings = Settings()) {
    private val json = Json { ignoreUnknownKeys = true }

    fun loadGame(): GameState? = read(GAME_KEY)

    fun saveGame(game: GameState?) {
        if (game == null) settings.remove(GAME_KEY) else settings.putString(GAME_KEY, json.encodeToString(game))
    }

    fun loadStats(): Stats = read(STATS_KEY) ?: Stats()

    fun saveStats(stats: Stats) = settings.putString(STATS_KEY, json.encodeToString(stats))

    // A save from an incompatible older version is dropped rather than crashing the app.
    private inline fun <reified T> read(key: String): T? =
        settings.getStringOrNull(key)?.let { runCatching { json.decodeFromString<T>(it) }.getOrNull() }

    private companion object {
        const val GAME_KEY = "game.v1"
        const val STATS_KEY = "stats.v1"
    }
}
