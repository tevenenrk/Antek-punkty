package pl.antek.punkty.ui

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.Badge
import pl.antek.punkty.data.Behavior
import pl.antek.punkty.data.Defaults
import pl.antek.punkty.data.EarnedBadge
import pl.antek.punkty.data.EventKind
import pl.antek.punkty.data.PointEvent
import pl.antek.punkty.data.Repository
import pl.antek.punkty.data.Reward
import pl.antek.punkty.data.newId
import pl.antek.punkty.logic.BadgeEngine
import pl.antek.punkty.logic.Level
import pl.antek.punkty.logic.Levels
import pl.antek.punkty.sync.FamilyCode
import pl.antek.punkty.sync.FamilySync
import pl.antek.punkty.sync.SyncBackend
import pl.antek.punkty.sync.FirebaseProvider
import pl.antek.punkty.sync.SyncCodec
import pl.antek.punkty.sync.normalized

sealed interface Celebration {
    data class BadgeEarned(val badge: Badge) : Celebration
    data class LevelUp(val level: Level) : Celebration
    data class RewardRedeemed(val reward: Reward) : Celebration
}

/** Stan synchronizacji widoczny w UI. */
data class SyncUi(
    val available: Boolean,
    val code: String? = null,
    val online: Boolean = false,
    val busy: Boolean = false,
    val error: String? = null,
)

class AppDeps(val repo: Repository, val prefs: SharedPreferences, val sync: SyncBackend?) {
    companion object {
        fun default(app: Application) = AppDeps(
            Repository(app.filesDir),
            app.getSharedPreferences("sync", Context.MODE_PRIVATE),
            FirebaseProvider.database(app)?.let { FamilySync(it) },
        )
    }
}

class AppViewModel(app: Application, deps: AppDeps) : AndroidViewModel(app) {
    constructor(app: Application) : this(app, AppDeps.default(app))

    private val repo = deps.repo
    private val prefs = deps.prefs
    private val sync: SyncBackend? = deps.sync
    private val _data = MutableStateFlow(repo.load())
    val data: StateFlow<AppData> = _data.asStateFlow()

    private val unlocked = MutableStateFlow(false)
    val isParent: StateFlow<Boolean> = combine(_data, unlocked) { d, u -> d.pin.isNullOrEmpty() || u }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _data.value.pin.isNullOrEmpty())

    private val _celebrations = Channel<Celebration>(Channel.UNLIMITED)
    val celebrations: Flow<Celebration> = _celebrations.receiveAsFlow()

    private val saveMutex = Mutex()

    private val _sync = MutableStateFlow(SyncUi(available = sync != null))
    val syncState: StateFlow<SyncUi> = _sync.asStateFlow()
    private val familyCode: String? get() = _sync.value.code

    init {
        refresh()
        sync?.watchConnection { online -> _sync.value = _sync.value.copy(online = online) }
        prefs.getString(KEY_CODE, null)?.let { code ->
            if (sync != null) startListening(code)
        }
    }

    override fun onCleared() {
        sync?.close()
    }

    /** Ponowne sprawdzenie odznak (np. „bez wpadki” zależy od upływu dni). */
    fun refresh() = update { it }

    private fun update(transform: (AppData) -> AppData) {
        val before = _data.value
        val after = awardBadges(transform(before))
        if (after == before) return
        commit(before, after)
        if (familyCode != null) sync?.push(SyncCodec.diff(before, after))
    }

    private fun commit(before: AppData, after: AppData) {
        _data.value = after
        val lb = Levels.forXp(before.xp)
        val la = Levels.forXp(after.xp)
        if (la.number > lb.number) _celebrations.trySend(Celebration.LevelUp(la))
        persist()
    }

    /** Zmiana przyszła z innego telefonu. */
    private fun applyRemote(remote: AppData) {
        val before = _data.value
        if (remote == before.normalized()) return
        val known = before.earned.map { it.badgeId }.toSet()
        remote.earned.filter { it.badgeId !in known }.forEach { e ->
            remote.badges.find { it.id == e.badgeId }?.let { _celebrations.trySend(Celebration.BadgeEarned(it)) }
        }
        val after = awardBadges(remote)
        commit(before, after)
        if (after != remote) sync?.push(SyncCodec.diff(remote, after))
    }

    // --- wspólne punkty na kilku telefonach ---

    private fun startListening(code: String) {
        val s = sync ?: return
        _sync.value = _sync.value.copy(code = code, error = null)
        s.start(code, onData = ::applyRemote, onError = { msg ->
            _sync.value = _sync.value.copy(error = "Synchronizacja przerwana: $msg")
        })
    }

    private fun saveCode(code: String?) {
        prefs.edit().apply { if (code == null) remove(KEY_CODE) else putString(KEY_CODE, code) }.apply()
    }

    /** Ten telefon zakłada rodzinę i wysyła do niej swoje dane. */
    fun createFamily() {
        val s = sync ?: return
        val code = FamilyCode.generate()
        s.create(code, _data.value.normalized()) { msg ->
            _sync.value = _sync.value.copy(error = "Nie udało się utworzyć kodu: $msg")
        }
        saveCode(code)
        startListening(code)
    }

    /** Ten telefon dołącza do rodziny – jego dane zostają zastąpione danymi z chmury. */
    fun joinFamily(input: String) {
        val s = sync ?: return
        val code = FamilyCode.normalize(input)
        if (code == null) {
            _sync.value = _sync.value.copy(error = "Kod powinien mieć ${FamilyCode.LENGTH} znaków, np. ABCDE-FGH23")
            return
        }
        _sync.value = _sync.value.copy(busy = true, error = null)
        s.fetch(
            code,
            onResult = { remote ->
                if (remote == null) {
                    _sync.value = _sync.value.copy(busy = false, error = "Nie znaleziono rodziny o kodzie ${FamilyCode.pretty(code)}")
                } else {
                    _data.value = remote
                    persist()
                    saveCode(code)
                    _sync.value = _sync.value.copy(busy = false)
                    startListening(code)
                }
            },
            onError = { msg ->
                _sync.value = _sync.value.copy(busy = false, error = "Brak połączenia z internetem ($msg)")
            },
        )
    }

    /** Odłącza ten telefon. Dane zostają na telefonie jako osobna kopia. */
    fun leaveFamily() {
        sync?.stop()
        saveCode(null)
        _sync.value = _sync.value.copy(code = null, error = null)
    }

    fun clearSyncError() {
        _sync.value = _sync.value.copy(error = null)
    }

    private fun awardBadges(data: AppData): AppData {
        val newly = BadgeEngine.newlyEarned(data)
        if (newly.isEmpty()) return data
        val now = System.currentTimeMillis()
        val bonus = newly.filter { it.bonus > 0 }.map {
            // Stałe id: gdy dwa telefony jednocześnie przyznają tę samą odznakę, bonus nie zdubluje się.
            PointEvent("badge-${it.id}", now, EventKind.BADGE, it.id, "Odznaka: ${it.name}", it.icon, it.bonus)
        }
        newly.forEach { _celebrations.trySend(Celebration.BadgeEarned(it)) }
        return data.copy(
            earned = data.earned + newly.map { EarnedBadge(it.id, now) },
            events = data.events + bonus,
        )
    }

    private fun persist() {
        viewModelScope.launch(Dispatchers.IO) {
            saveMutex.withLock { repo.save(_data.value) }
        }
    }

    // --- punkty ---

    fun logBehavior(b: Behavior): PointEvent {
        val ev = PointEvent(newId(), System.currentTimeMillis(), EventKind.BEHAVIOR, b.id, b.name, b.icon, b.signedPoints)
        update { it.copy(events = it.events + ev) }
        return ev
    }

    fun removeEvent(id: String) = update { d -> d.copy(events = d.events.filterNot { it.id == id }) }

    fun redeem(r: Reward): Boolean {
        if (_data.value.balance < r.cost) return false
        val ev = PointEvent(newId(), System.currentTimeMillis(), EventKind.REWARD, r.id, r.name, r.icon, -r.cost)
        update { it.copy(events = it.events + ev) }
        _celebrations.trySend(Celebration.RewardRedeemed(r))
        return true
    }

    fun manualAdjust(points: Int, note: String) {
        if (points == 0) return
        val title = note.trim().ifBlank { if (points > 0) "Bonus od rodzica" else "Korekta punktów" }
        val ev = PointEvent(
            newId(), System.currentTimeMillis(), EventKind.MANUAL, null, title,
            if (points > 0) "🎁" else "✂️", points,
        )
        update { it.copy(events = it.events + ev) }
    }

    // --- konfiguracja ---

    fun saveBehavior(b: Behavior) = update { d -> d.copy(behaviors = d.behaviors.upsert(b) { it.id }) }

    fun deleteBehavior(id: String) = update { d ->
        d.copy(
            behaviors = d.behaviors.filterNot { it.id == id },
            badges = d.badges.map { bd -> bd.copy(items = bd.items.filterNot { it.behaviorId == id }) },
        )
    }

    fun saveReward(r: Reward) = update { d -> d.copy(rewards = d.rewards.upsert(r) { it.id }) }
    fun deleteReward(id: String) = update { d -> d.copy(rewards = d.rewards.filterNot { it.id == id }) }

    fun saveBadge(b: Badge) = update { d -> d.copy(badges = d.badges.upsert(b) { it.id }) }
    fun deleteBadge(id: String) = update { d ->
        d.copy(badges = d.badges.filterNot { it.id == id }, earned = d.earned.filterNot { it.badgeId == id })
    }

    fun setChildName(name: String) = update { it.copy(childName = name.trim().ifBlank { "Antek" }) }

    fun setPin(pin: String?) {
        update { it.copy(pin = pin?.takeIf { p -> p.isNotBlank() }) }
        unlocked.value = pin != null
    }

    fun unlock(pin: String): Boolean {
        val ok = pin.isNotEmpty() && pin == _data.value.pin
        if (ok) unlocked.value = true
        return ok
    }

    fun lock() {
        unlocked.value = false
    }

    fun resetProgress() {
        val now = System.currentTimeMillis()
        update { d ->
            d.copy(
                events = emptyList(),
                earned = emptyList(),
                createdAt = now,
                badges = d.badges.map { it.copy(createdAt = now) },
            )
        }
    }

    fun resetAll() = update { d -> Defaults.create().copy(pin = d.pin, childName = d.childName) }
}

private const val KEY_CODE = "family_code"

fun <T> List<T>.upsert(item: T, key: (T) -> Any): List<T> {
    val k = key(item)
    return if (any { key(it) == k }) map { if (key(it) == k) item else it } else this + item
}
