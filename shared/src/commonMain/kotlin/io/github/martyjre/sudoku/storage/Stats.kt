package io.github.martyjre.sudoku.storage

import io.github.martyjre.sudoku.engine.Difficulty
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class DifficultySummary(
    val difficulty: Difficulty,
    val solved: Int,
    val bestSeconds: Long?,
    val averageSeconds: Long?,
)

data class DayCount(val date: LocalDate, val count: Int)

data class Summary(
    val solved: Int,
    val bestSeconds: Long?,
    val averageSeconds: Long?,
    val currentStreak: Int,
    val longestStreak: Int,
    val byDifficulty: List<DifficultySummary>,
    /** Most recent solves, oldest first, for the solve-time trend. */
    val recent: List<GameRecord>,
    val perDay: List<DayCount>,
)

fun GameRecord.date(zone: TimeZone): LocalDate =
    Instant.fromEpochMilliseconds(finishedAtMillis).toLocalDateTime(zone).date

/**
 * Streaks count consecutive days with at least one solve of this game; the current one survives
 * until the end of today so it doesn't reset the moment midnight passes.
 */
fun summarize(
    history: List<GameRecord>,
    kind: GameKind,
    difficulty: Difficulty?,
    today: LocalDate,
    zone: TimeZone,
    recentCount: Int = 30,
    days: Int = 30,
): Summary {
    val games = history.filter { it.kind == kind }
    val shown = games.filter { difficulty == null || it.difficulty == difficulty }
    val playedDays = games.map { it.date(zone) }.toSet()

    var current = 0
    var day = if (today in playedDays) today else today.minus(DatePeriod(days = 1))
    while (day in playedDays) {
        current++
        day = day.minus(DatePeriod(days = 1))
    }

    var longest = 0
    var run = 0
    var previous: LocalDate? = null
    for (d in playedDays.sorted()) {
        run = if (previous != null && d.minus(DatePeriod(days = 1)) == previous) run + 1 else 1
        longest = maxOf(longest, run)
        previous = d
    }

    val counts = shown.groupingBy { it.date(zone) }.eachCount()
    val perDay = (days - 1 downTo 0).map { back ->
        val date = today.minus(DatePeriod(days = back))
        DayCount(date, counts[date] ?: 0)
    }

    return Summary(
        solved = shown.size,
        bestSeconds = shown.minOfOrNull { it.seconds },
        averageSeconds = shown.takeIf { it.isNotEmpty() }?.let { list -> list.sumOf { it.seconds } / list.size },
        currentStreak = current,
        longestStreak = longest,
        byDifficulty = Difficulty.entries.map { d ->
            val list = games.filter { it.difficulty == d }
            DifficultySummary(
                difficulty = d,
                solved = list.size,
                bestSeconds = list.minOfOrNull { it.seconds },
                averageSeconds = list.takeIf { it.isNotEmpty() }?.let { l -> l.sumOf { it.seconds } / l.size },
            )
        },
        recent = shown.sortedBy { it.finishedAtMillis }.takeLast(recentCount),
        perDay = perDay,
    )
}
