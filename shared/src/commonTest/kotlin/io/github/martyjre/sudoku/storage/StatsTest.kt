package io.github.martyjre.sudoku.storage

import com.russhwolf.settings.MapSettings
import io.github.martyjre.sudoku.engine.Difficulty
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsTest {
    private val zone = TimeZone.UTC
    private val today = LocalDate(2026, 10, 27)

    private fun record(date: LocalDate, seconds: Long, difficulty: Difficulty = Difficulty.EASY, kind: GameKind = GameKind.SUDOKU) =
        GameRecord(kind, difficulty, seconds, hints = 0, finishedAtMillis = date.atStartOfDayIn(zone).toEpochMilliseconds() + 3_600_000)

    @Test
    fun summarizesTimesAndStreaks() {
        val history = listOf(
            record(LocalDate(2026, 10, 20), 600),
            record(LocalDate(2026, 10, 21), 500),
            record(LocalDate(2026, 10, 22), 400, Difficulty.HARD),
            record(LocalDate(2026, 10, 25), 300),
            record(LocalDate(2026, 10, 26), 200),
            record(LocalDate(2026, 10, 26), 100, kind = GameKind.CATS),
        )
        val all = summarize(history, GameKind.SUDOKU, difficulty = null, today = today, zone = zone)
        assertEquals(5, all.solved)
        assertEquals(200, all.bestSeconds)
        assertEquals(400, all.averageSeconds)
        assertEquals(2, all.currentStreak, "yesterday still counts until today ends")
        assertEquals(3, all.longestStreak)
        assertEquals(30, all.perDay.size)
        assertEquals(1, all.perDay.last { it.date == LocalDate(2026, 10, 26) }.count)

        val easy = summarize(history, GameKind.SUDOKU, Difficulty.EASY, today, zone)
        assertEquals(4, easy.solved)
        assertEquals(1, easy.byDifficulty.first { it.difficulty == Difficulty.HARD }.solved)

        val cats = summarize(history, GameKind.CATS, null, today, zone)
        assertEquals(1, cats.solved)
    }

    @Test
    fun historyPersists() {
        val storage = Storage(MapSettings())
        storage.addRecord(record(today, 42))
        storage.addRecord(record(today, 43, kind = GameKind.CATS))
        assertEquals(listOf(42L, 43L), storage.loadHistory().map { it.seconds })
    }
}
