package pl.antek.punkty.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.Badge
import pl.antek.punkty.data.Behavior
import pl.antek.punkty.data.EarnedBadge
import pl.antek.punkty.data.PointEvent
import pl.antek.punkty.data.Reward
import java.security.SecureRandom

/**
 * Układ danych w chmurze (Firebase Realtime Database):
 *
 * families/{KOD}/config          – JSON: imię, PIN, zachowania, nagrody, odznaki
 * families/{KOD}/events/{id}     – JSON jednego wpisu punktów (dopisywane osobno, więc dwa telefony się nie nadpisują)
 * families/{KOD}/earned/{badge}  – czas zdobycia odznaki
 */
@Serializable
data class SyncConfig(
    val childName: String,
    val pin: String? = null,
    val behaviors: List<Behavior> = emptyList(),
    val rewards: List<Reward> = emptyList(),
    val badges: List<Badge> = emptyList(),
    val createdAt: Long = 0,
)

object SyncCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private fun config(d: AppData) = SyncConfig(d.childName, d.pin, d.behaviors, d.rewards, d.badges, d.createdAt)
    fun configJson(d: AppData): String = json.encodeToString(SyncConfig.serializer(), config(d))
    fun eventJson(e: PointEvent): String = json.encodeToString(PointEvent.serializer(), e)

    /** Cały stan – przy zakładaniu rodziny. */
    fun full(d: AppData): Map<String, Any?> = mapOf(
        "config" to configJson(d),
        "events" to d.events.associate { it.id to eventJson(it) },
        "earned" to d.earned.associate { it.badgeId to it.time },
    )

    /** Tylko zmiany (ścieżki względem families/{KOD}); null = usunięcie. */
    fun diff(before: AppData, after: AppData): Map<String, Any?> {
        val m = linkedMapOf<String, Any?>()
        if (config(before) != config(after)) m["config"] = configJson(after)

        val b = before.events.associateBy { it.id }
        val a = after.events.associateBy { it.id }
        for ((id, e) in a) if (b[id] != e) m["events/$id"] = eventJson(e)
        for (id in b.keys - a.keys) m["events/$id"] = null

        val be = before.earned.associateBy { it.badgeId }
        val ae = after.earned.associateBy { it.badgeId }
        for ((id, e) in ae) if (be[id] != e) m["earned/$id"] = e.time
        for (id in be.keys - ae.keys) m["earned/$id"] = null
        return m
    }

    fun decode(configJson: String?, events: Collection<String>, earned: Map<String, Long>): AppData? {
        val c = configJson?.let { runCatching { json.decodeFromString(SyncConfig.serializer(), it) }.getOrNull() }
            ?: return null
        return AppData(
            childName = c.childName,
            pin = c.pin,
            behaviors = c.behaviors,
            rewards = c.rewards,
            badges = c.badges,
            events = events.mapNotNull { runCatching { json.decodeFromString(PointEvent.serializer(), it) }.getOrNull() },
            earned = earned.map { (id, t) -> EarnedBadge(id, t) },
            createdAt = c.createdAt,
        ).normalized()
    }
}

/** Kolejność wpisów niezależna od tego, który telefon co pierwszy zapisał. */
fun AppData.normalized(): AppData = copy(
    events = events.sortedWith(compareBy<PointEvent>({ it.time }, { it.id })),
    earned = earned.sortedWith(compareBy<EarnedBadge>({ it.time }, { it.badgeId })),
)

object FamilyCode {
    /** Bez znaków łatwych do pomylenia (0/O, 1/I). 32^10 ≈ 10^15 kombinacji. */
    private const val ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    const val LENGTH = 10
    private val random = SecureRandom()

    fun generate(): String = String(CharArray(LENGTH) { ALPHABET[random.nextInt(ALPHABET.length)] })

    fun normalize(input: String): String? {
        val s = input.uppercase().filter { it in ALPHABET }
        return s.takeIf { it.length == LENGTH }
    }

    fun pretty(code: String): String = code.take(5) + "-" + code.drop(5)
}
