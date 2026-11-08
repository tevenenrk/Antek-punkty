package pl.antek.punkty.logic

import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.Badge
import pl.antek.punkty.data.BadgeItem
import pl.antek.punkty.data.BadgeType
import pl.antek.punkty.data.Behavior
import pl.antek.punkty.data.EventKind
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

data class BadgeProgress(val current: Int, val target: Int) {
    val done: Boolean get() = current >= target
    val fraction: Float get() = if (target <= 0) 1f else (current.toFloat() / target).coerceIn(0f, 1f)
}

val BadgeType.emoji: String
    get() = when (this) {
        BadgeType.COMBO_DAY -> "🎯"
        BadgeType.STREAK -> "🔥"
        BadgeType.TOTAL -> "🧮"
        BadgeType.ABSTAIN -> "🛡️"
    }

val BadgeType.title: String
    get() = when (this) {
        BadgeType.COMBO_DAY -> "Combo dnia"
        BadgeType.STREAK -> "Seria dni z rzędu"
        BadgeType.TOTAL -> "Licznik"
        BadgeType.ABSTAIN -> "Bez wpadki"
    }

val BadgeType.explanation: String
    get() = when (this) {
        BadgeType.COMBO_DAY -> "Kilka wybranych zachowań wykonanych tego samego dnia"
        BadgeType.STREAK -> "Wybrane zachowania codziennie przez kilka dni z rzędu"
        BadgeType.TOTAL -> "Wybrane zachowania wykonane łącznie określoną liczbę razy"
        BadgeType.ABSTAIN -> "Kilka dni z rzędu bez wybranych złych zachowań"
    }

object BadgeEngine {
    private val zone: ZoneId get() = ZoneId.systemDefault()

    fun day(ts: Long): LocalDate = Instant.ofEpochMilli(ts).atZone(zone).toLocalDate()
    fun today(): LocalDate = LocalDate.now(zone)
    fun isToday(ts: Long) = day(ts) == today()

    private fun daysBetween(a: LocalDate, b: LocalDate) = ChronoUnit.DAYS.between(a, b).toInt()

    /** dzień -> (id zachowania -> liczba wykonań) */
    private fun countsByDay(data: AppData): Map<LocalDate, Map<String, Int>> =
        data.events
            .filter { it.kind == EventKind.BEHAVIOR && it.refId != null }
            .groupBy { day(it.time) }
            .mapValues { (_, evs) -> evs.groupingBy { it.refId!! }.eachCount() }

    private fun daySatisfies(counts: Map<String, Int>?, items: List<BadgeItem>): Boolean =
        items.isNotEmpty() && counts != null && items.all { (counts[it.behaviorId] ?: 0) >= it.count }

    fun longestRun(days: Set<LocalDate>): Int {
        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in days.sorted()) {
            run = if (prev != null && daysBetween(prev, d) == 1) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }
        return best
    }

    /** Seria kończąca się dziś (lub wczoraj, jeśli dziś jeszcze nic nie było). */
    fun currentRun(days: Set<LocalDate>): Int {
        var d = today()
        if (d !in days) d = d.minusDays(1)
        var n = 0
        while (d in days) {
            n++
            d = d.minusDays(1)
        }
        return n
    }

    fun behaviorStreaks(data: AppData): Map<String, Int> {
        val daysByBehavior = data.events
            .filter { it.kind == EventKind.BEHAVIOR && it.refId != null }
            .groupBy({ it.refId!! }, { day(it.time) })
        return daysByBehavior.mapValues { (_, days) -> currentRun(days.toSet()) }
    }

    fun progress(badge: Badge, data: AppData): BadgeProgress {
        if (badge.items.isEmpty()) return BadgeProgress(0, 1)
        val target = badge.target.coerceAtLeast(1)
        return when (badge.type) {
            BadgeType.COMBO_DAY -> {
                val byDay = countsByDay(data)
                val size = badge.items.size
                if (byDay.values.any { daySatisfies(it, badge.items) }) {
                    BadgeProgress(size, size)
                } else {
                    val todayCounts = byDay[today()]
                    BadgeProgress(
                        badge.items.count { (todayCounts?.get(it.behaviorId) ?: 0) >= it.count },
                        size,
                    )
                }
            }

            BadgeType.STREAK -> {
                val days = countsByDay(data).filter { daySatisfies(it.value, badge.items) }.keys
                if (longestRun(days) >= target) BadgeProgress(target, target)
                else BadgeProgress(currentRun(days), target)
            }

            BadgeType.TOTAL -> {
                val ids = badge.items.map { it.behaviorId }.toSet()
                val n = data.events.count { it.kind == EventKind.BEHAVIOR && it.refId in ids }
                BadgeProgress(minOf(n, target), target)
            }

            BadgeType.ABSTAIN -> {
                val ids = badge.items.map { it.behaviorId }.toSet()
                val start = day(maxOf(badge.createdAt, data.createdAt))
                val today = today()
                val occurrences = data.events
                    .filter { it.kind == EventKind.BEHAVIOR && it.refId in ids }
                    .map { day(it.time) }
                    .filter { !it.isBefore(start) }
                    .toSortedSet()
                var best = 0
                var prev: LocalDate? = null
                for (d in occurrences) {
                    val gap = if (prev == null) daysBetween(start, d) else daysBetween(prev, d) - 1
                    best = maxOf(best, gap)
                    prev = d
                }
                val trailing = if (prev == null) daysBetween(start, today) else daysBetween(prev, today) - 1
                best = maxOf(best, trailing)
                if (best >= target) BadgeProgress(target, target)
                else BadgeProgress(trailing.coerceAtLeast(0), target)
            }
        }
    }

    fun newlyEarned(data: AppData): List<Badge> {
        val earnedIds = data.earned.map { it.badgeId }.toSet()
        return data.badges.filter { it.id !in earnedIds && progress(it, data).done }
    }

    fun progressUnit(type: BadgeType, n: Int): String = when (type) {
        BadgeType.COMBO_DAY -> "dziś"
        BadgeType.STREAK, BadgeType.ABSTAIN -> daysWord(n)
        BadgeType.TOTAL -> timesWord(n)
    }

    fun describe(badge: Badge, behaviors: List<Behavior>): String {
        if (badge.note.isNotBlank()) return badge.note
        val byId = behaviors.associateBy { it.id }
        val names = badge.items.mapNotNull { item ->
            val b = byId[item.behaviorId] ?: return@mapNotNull null
            val times = if (item.count > 1 && (badge.type == BadgeType.COMBO_DAY || badge.type == BadgeType.STREAK)) " ×${item.count}" else ""
            "${b.icon} ${b.name}$times"
        }
        if (names.isEmpty()) return "Wybierz zachowania w kreatorze odznak"
        val t = badge.target
        return when (badge.type) {
            BadgeType.COMBO_DAY -> "Jednego dnia: " + joinAnd(names)
            BadgeType.STREAK -> "Codziennie przez $t ${daysWord(t)} z rzędu: " + joinAnd(names)
            BadgeType.TOTAL -> "Łącznie $t ${timesWord(t)}: " + names.joinToString(" lub ")
            BadgeType.ABSTAIN -> "$t ${daysWord(t)} z rzędu bez: " + names.joinToString(", ")
        }
    }

    private fun joinAnd(list: List<String>): String =
        if (list.size <= 1) list.joinToString() else list.dropLast(1).joinToString(", ") + " i " + list.last()
}
