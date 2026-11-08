package pl.antek.punkty.data

import kotlinx.serialization.Serializable
import java.util.UUID

fun newId(): String = UUID.randomUUID().toString()

/** Zachowanie (dobre lub złe), które daje lub zabiera punkty. [points] to zawsze wartość dodatnia. */
@Serializable
data class Behavior(
    val id: String,
    val name: String,
    val icon: String,
    val points: Int,
    val positive: Boolean,
) {
    val signedPoints: Int get() = if (positive) points else -points
}

@Serializable
data class Reward(
    val id: String,
    val name: String,
    val icon: String,
    val cost: Int,
)

@Serializable
enum class BadgeType {
    /** Wszystkie wybrane zachowania (każde min. [BadgeItem.count] razy) tego samego dnia. */
    COMBO_DAY,

    /** Wybrane zachowania codziennie przez [Badge.target] dni z rzędu. */
    STREAK,

    /** Wybrane zachowania łącznie [Badge.target] razy. */
    TOTAL,

    /** [Badge.target] dni z rzędu bez wybranych (złych) zachowań. */
    ABSTAIN,
}

@Serializable
data class BadgeItem(
    val behaviorId: String,
    val count: Int = 1,
)

@Serializable
data class Badge(
    val id: String,
    val name: String,
    val icon: String,
    val type: BadgeType,
    val items: List<BadgeItem>,
    val target: Int = 1,
    val bonus: Int = 0,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
enum class EventKind { BEHAVIOR, REWARD, BADGE, MANUAL }

@Serializable
data class PointEvent(
    val id: String,
    val time: Long,
    val kind: EventKind,
    val refId: String? = null,
    val title: String,
    val icon: String,
    val points: Int,
)

@Serializable
data class EarnedBadge(
    val badgeId: String,
    val time: Long,
)

@Serializable
data class AppData(
    val childName: String = "Antek",
    val pin: String? = null,
    val behaviors: List<Behavior> = emptyList(),
    val rewards: List<Reward> = emptyList(),
    val badges: List<Badge> = emptyList(),
    val events: List<PointEvent> = emptyList(),
    val earned: List<EarnedBadge> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
) {
    /** Punkty do wydania (wszystko: zachowania, nagrody, bonusy, korekty). */
    val balance: Int get() = events.sumOf { it.points }

    /** Doświadczenie do poziomów – tylko zdobyte punkty, nie maleje przy odbieraniu nagród. */
    val xp: Int get() = events.filter { it.kind != EventKind.REWARD && it.points > 0 }.sumOf { it.points }
}
