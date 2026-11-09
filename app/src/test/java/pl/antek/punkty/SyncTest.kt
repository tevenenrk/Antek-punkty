package pl.antek.punkty

import android.app.Application
import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.Badge
import pl.antek.punkty.data.BadgeItem
import pl.antek.punkty.data.BadgeType
import pl.antek.punkty.data.Repository
import pl.antek.punkty.sync.FamilyCode
import pl.antek.punkty.sync.SyncBackend
import pl.antek.punkty.sync.SyncCodec
import pl.antek.punkty.ui.AppDeps
import pl.antek.punkty.ui.AppViewModel
import java.io.File
import java.nio.file.Files

/** Chmura w pamięci zachowująca się jak Realtime Database: drzewo ścieżek, aktualizacje wielościeżkowe, nasłuchiwanie. */
class FakeCloud {
    val families = mutableMapOf<String, MutableMap<String, Any?>>() // code -> {"config": json, "events/<id>": json, "earned/<id>": time}
    val listeners = mutableMapOf<String, MutableList<(AppData) -> Unit>>()

    fun snapshot(code: String): AppData? {
        val f = families[code] ?: return null
        return SyncCodec.decode(
            f["config"] as String?,
            f.filterKeys { it.startsWith("events/") }.values.map { it as String },
            f.filterKeys { it.startsWith("earned/") }.mapKeys { it.key.removePrefix("earned/") }.mapValues { it.value as Long },
        )
    }

    fun write(code: String, updates: Map<String, Any?>) {
        val f = families.getOrPut(code) { mutableMapOf() }
        updates.forEach { (k, v) -> if (v == null) f.remove(k) else f[k] = v }
        val snap = snapshot(code) ?: return
        listeners[code].orEmpty().toList().forEach { it(snap) }
    }

    fun backend() = object : SyncBackend {
        var code: String? = null
        var cb: ((AppData) -> Unit)? = null
        override fun watchConnection(onChange: (Boolean) -> Unit) = onChange(true)
        override fun start(code: String, onData: (AppData) -> Unit, onError: (String) -> Unit) {
            stop(); this.code = code; cb = onData
            listeners.getOrPut(code) { mutableListOf() } += onData
            snapshot(code)?.let(onData)
        }
        override fun stop() { code?.let { c -> listeners[c]?.remove(cb) }; code = null; cb = null }
        override fun close() = stop()
        override fun push(updates: Map<String, Any?>) { code?.let { write(it, updates) } }
        override fun create(code: String, data: AppData, onError: (String) -> Unit) {
            families[code] = mutableMapOf()
            val flat = mutableMapOf<String, Any?>("config" to SyncCodec.configJson(data))
            data.events.forEach { flat["events/${it.id}"] = SyncCodec.eventJson(it) }
            data.earned.forEach { flat["earned/${it.badgeId}"] = it.time }
            write(code, flat)
        }
        override fun fetch(code: String, onResult: (AppData?) -> Unit, onError: (String) -> Unit) = onResult(snapshot(code))
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SyncTest {
    private lateinit var app: Application
    private val cloud = FakeCloud()

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
    }

    private fun phone(name: String): AppViewModel {
        val dir = Files.createTempDirectory(name).toFile()
        return AppViewModel(app, AppDeps(Repository(dir), app.getSharedPreferences(name, Context.MODE_PRIVATE), cloud.backend()))
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun twoPhonesShareEverything() {
        val a = phone("a")
        val b = phone("b")
        val teeth = a.data.value.behaviors.first { it.id == "b_teeth" }
        a.logBehavior(teeth)

        a.createFamily()
        val code = a.syncState.value.code
        assertNotNull(code)

        // B ma własne, inne dane – po dołączeniu dostaje dane rodziny
        b.logBehavior(b.data.value.behaviors.first { it.id == "n_anger" })
        b.joinFamily(FamilyCode.pretty(code!!).lowercase())
        idle()
        assertEquals(code, b.syncState.value.code)
        assertEquals(a.data.value.balance, b.data.value.balance)
        assertEquals(2, b.data.value.balance)

        // punkty w obie strony
        b.logBehavior(teeth)
        a.logBehavior(a.data.value.behaviors.first { it.id == "b_homework" })
        idle()
        assertEquals(9, a.data.value.balance)
        assertEquals(9, b.data.value.balance)

        // cofnięcie na jednym telefonie znika na drugim
        val last = b.data.value.events.last()
        a.removeEvent(last.id)
        idle()
        assertEquals(a.data.value.balance, b.data.value.balance)

        // odebranie nagrody, zmiana konfiguracji i imienia
        a.manualAdjust(30, "test")
        b.redeem(b.data.value.rewards.first { it.id == "r_cartoon" })
        a.setChildName("Antoś")
        b.saveBehavior(teeth.copy(points = 5))
        idle()
        assertEquals("Antoś", b.data.value.childName)
        assertEquals(5, a.data.value.behaviors.first { it.id == "b_teeth" }.points)
        assertEquals(a.data.value.balance, b.data.value.balance)
        assertEquals(a.data.value.events.map { it.id }.sorted(), b.data.value.events.map { it.id }.sorted())
    }

    @Test
    fun badgeBonusIsNotDuplicatedWhenBothPhonesAward() {
        val a = phone("c")
        val b = phone("d")
        a.saveBadge(Badge("gx", "Test", "🏅", BadgeType.TOTAL, listOf(BadgeItem("b_bed")), target = 1, bonus = 10))
        a.createFamily()
        b.joinFamily(a.syncState.value.code!!)
        idle()
        a.logBehavior(a.data.value.behaviors.first { it.id == "b_bed" })
        idle()
        val bonuses = b.data.value.events.filter { it.id == "badge-gx" }
        assertEquals(1, bonuses.size)
        assertEquals(1, b.data.value.earned.count { it.badgeId == "gx" })
        assertEquals(12, b.data.value.balance)
        assertEquals(12, a.data.value.balance)
    }

    @Test
    fun wrongCodeAndLeaving() {
        val a = phone("e")
        a.joinFamily("ABC")
        assertNotNull(a.syncState.value.error)
        a.joinFamily("ZZZZZ-ZZZZZ")
        assertNotNull(a.syncState.value.error)
        assertNull(a.syncState.value.code)

        a.createFamily()
        val code = a.syncState.value.code!!
        a.leaveFamily()
        assertNull(a.syncState.value.code)
        a.logBehavior(a.data.value.behaviors.first())
        assertEquals(0, cloud.snapshot(code)!!.events.size)
    }

    @Test
    fun codeSurvivesRestart() {
        val prefs = app.getSharedPreferences("f", Context.MODE_PRIVATE)
        val dir = Files.createTempDirectory("f").toFile()
        val a = AppViewModel(app, AppDeps(Repository(dir), prefs, cloud.backend()))
        a.createFamily()
        val code = a.syncState.value.code
        val again = AppViewModel(app, AppDeps(Repository(dir), prefs, cloud.backend()))
        assertEquals(code, again.syncState.value.code)
    }

    @Test
    fun diffIsMinimal() {
        val d = AppData(childName = "A")
        val d2 = d.copy(events = listOf(pl.antek.punkty.data.PointEvent("x", 1, pl.antek.punkty.data.EventKind.MANUAL, null, "t", "🎁", 3)))
        assertEquals(setOf("events/x"), SyncCodec.diff(d, d2).keys)
        assertEquals(mapOf("events/x" to null), SyncCodec.diff(d2, d))
        assertEquals(setOf("config"), SyncCodec.diff(d, d.copy(childName = "B")).keys)
        assertEquals(FamilyCode.LENGTH, FamilyCode.generate().length)
        assertEquals("ABCDEFGH23", FamilyCode.normalize("abcde-fgh23"))
    }
}
