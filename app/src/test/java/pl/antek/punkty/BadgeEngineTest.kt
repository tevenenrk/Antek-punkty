package pl.antek.punkty

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.Badge
import pl.antek.punkty.data.BadgeItem
import pl.antek.punkty.data.BadgeType
import pl.antek.punkty.data.Behavior
import pl.antek.punkty.data.EventKind
import pl.antek.punkty.data.PointEvent
import pl.antek.punkty.logic.BadgeEngine
import pl.antek.punkty.logic.Levels
import pl.antek.punkty.logic.pointsWord
import java.time.LocalDate
import java.time.ZoneId

class BadgeEngineTest {
    private val teeth = Behavior("teeth", "Zęby", "🦷", 2, true)
    private val bed = Behavior("bed", "Łóżko", "🛏️", 2, true)
    private val anger = Behavior("anger", "Złość", "😡", 3, false)
    private val today = LocalDate.now()
    private var seq = 0

    private fun at(daysAgo: Long, hour: Int = 10): Long =
        today.minusDays(daysAgo).atTime(hour, seq % 60).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private fun ev(b: Behavior, daysAgo: Long, hour: Int = 10) =
        PointEvent("e${seq++}", at(daysAgo, hour), EventKind.BEHAVIOR, b.id, b.name, b.icon, b.signedPoints)

    private fun data(vararg events: PointEvent, createdDaysAgo: Long = 30) = AppData(
        behaviors = listOf(teeth, bed, anger),
        events = events.toList(),
        createdAt = at(createdDaysAgo, 0),
    )

    private fun badge(type: BadgeType, items: List<BadgeItem>, target: Int = 1, createdDaysAgo: Long = 30) =
        Badge("x", "X", "🏅", type, items, target, createdAt = at(createdDaysAgo, 0))

    @Test
    fun comboDayRequiresAllOnSameDay() {
        val b = badge(BadgeType.COMBO_DAY, listOf(BadgeItem("teeth", 2), BadgeItem("bed")))
        assertFalse(BadgeEngine.progress(b, data(ev(teeth, 0), ev(bed, 0))).done)
        assertFalse(BadgeEngine.progress(b, data(ev(teeth, 1), ev(teeth, 0), ev(bed, 0))).done)
        assertTrue(BadgeEngine.progress(b, data(ev(teeth, 0, 8), ev(teeth, 0, 20), ev(bed, 0))).done)
    }

    @Test
    fun streakNeedsConsecutiveDays() {
        val b = badge(BadgeType.STREAK, listOf(BadgeItem("teeth")), target = 3)
        assertFalse(BadgeEngine.progress(b, data(ev(teeth, 0), ev(teeth, 1), ev(teeth, 3))).done)
        assertEquals(2, BadgeEngine.progress(b, data(ev(teeth, 0), ev(teeth, 1), ev(teeth, 3))).current)
        assertTrue(BadgeEngine.progress(b, data(ev(teeth, 5), ev(teeth, 6), ev(teeth, 7))).done)
        // dziś jeszcze nic – seria z wczoraj się liczy
        assertEquals(2, BadgeEngine.progress(b, data(ev(teeth, 1), ev(teeth, 2))).current)
    }

    @Test
    fun totalCountsAllOccurrences() {
        val b = badge(BadgeType.TOTAL, listOf(BadgeItem("teeth"), BadgeItem("bed")), target = 3)
        assertFalse(BadgeEngine.progress(b, data(ev(teeth, 0), ev(bed, 3))).done)
        assertTrue(BadgeEngine.progress(b, data(ev(teeth, 0), ev(bed, 3), ev(teeth, 9))).done)
    }

    @Test
    fun abstainCountsCleanDaysSinceBadgeCreated() {
        val b = badge(BadgeType.ABSTAIN, listOf(BadgeItem("anger")), target = 5, createdDaysAgo = 3)
        assertFalse(BadgeEngine.progress(b, data()).done)
        val b2 = badge(BadgeType.ABSTAIN, listOf(BadgeItem("anger")), target = 5, createdDaysAgo = 10)
        assertTrue(BadgeEngine.progress(b2, data()).done)
        // złość 2 dni temu -> tylko 1 pełny czysty dzień (wczoraj)
        val p = BadgeEngine.progress(b2, data(ev(anger, 2), ev(anger, 6), ev(anger, 9)))
        assertFalse(p.done)
        assertEquals(1, p.current)
    }

    @Test
    fun newlyEarnedSkipsAlreadyEarned() {
        val b = badge(BadgeType.TOTAL, listOf(BadgeItem("teeth")), target = 1)
        val d = data(ev(teeth, 0)).copy(badges = listOf(b))
        assertEquals(1, BadgeEngine.newlyEarned(d).size)
        assertEquals(0, BadgeEngine.newlyEarned(d.copy(earned = listOf(pl.antek.punkty.data.EarnedBadge("x", 0)))).size)
    }

    @Test
    fun levelsAndPolish() {
        assertEquals(1, Levels.forXp(0).number)
        assertEquals(2, Levels.forXp(30).number)
        assertEquals(12, Levels.forXp(99999).number)
        assertEquals("punkt", pointsWord(1))
        assertEquals("punkty", pointsWord(3))
        assertEquals("punktów", pointsWord(12))
        assertEquals("punkty", pointsWord(22))
        assertEquals("punktów", pointsWord(25))
    }

    @Test
    fun xpIgnoresRewardsButBalanceDoesNot() {
        val d = data(ev(teeth, 0), ev(anger, 0)).let {
            it.copy(events = it.events + PointEvent("r", at(0), EventKind.REWARD, "r", "Lody", "🍦", -1))
        }
        assertEquals(2, d.xp)
        assertEquals(2 - 3 - 1, d.balance)
    }
}
