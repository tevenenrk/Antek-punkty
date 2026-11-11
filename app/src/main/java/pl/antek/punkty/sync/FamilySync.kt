package pl.antek.punkty.sync

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import pl.antek.punkty.data.AppData

/** Jedna instancja bazy na proces (setPersistenceEnabled można wywołać tylko raz). */
object FirebaseProvider {
    @Volatile
    private var cached: FirebaseDatabase? = null
    private var tried = false

    @Synchronized
    fun database(context: Context): FirebaseDatabase? {
        if (tried) return cached
        tried = true
        cached = runCatching {
            val app = FirebaseApp.getApps(context).firstOrNull() ?: FirebaseApp.initializeApp(context)
            app?.let { FirebaseDatabase.getInstance(it).apply { setPersistenceEnabled(true) } }
        }.getOrNull()
        return cached
    }
}

/** Chmura, przez którą telefony wymieniają dane (Firebase w aplikacji, atrapa w testach). */
interface SyncBackend {
    fun watchConnection(onChange: (Boolean) -> Unit)
    fun start(code: String, onData: (AppData) -> Unit, onError: (String) -> Unit)
    fun stop()
    fun close()
    fun push(updates: Map<String, Any?>)
    fun create(code: String, data: AppData, onError: (String) -> Unit)
    fun fetch(code: String, onResult: (AppData?) -> Unit, onError: (String) -> Unit)
}

/** Cienka warstwa nad Firebase: nasłuchiwanie rodziny, wysyłanie zmian, tworzenie i dołączanie. */
class FamilySync(private val db: FirebaseDatabase) : SyncBackend {
    private var ref: DatabaseReference? = null
    private var listener: ValueEventListener? = null
    private var connectedRef: DatabaseReference? = null
    private var connectedListener: ValueEventListener? = null

    private fun family(code: String) = db.getReference("families").child(code)

    override fun watchConnection(onChange: (Boolean) -> Unit) {
        if (connectedListener != null) return
        val r = db.getReference(".info/connected")
        connectedListener = r.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) = onChange(s.getValue(Boolean::class.java) == true)
            override fun onCancelled(e: DatabaseError) = Unit
        })
        connectedRef = r
    }

    override fun start(code: String, onData: (AppData) -> Unit, onError: (String) -> Unit) {
        stop()
        val r = family(code)
        r.keepSynced(true)
        listener = r.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                decode(s)?.let(onData)
            }

            override fun onCancelled(e: DatabaseError) = onError(e.message)
        })
        ref = r
    }

    override fun stop() {
        listener?.let { ref?.removeEventListener(it) }
        ref?.keepSynced(false)
        ref = null
        listener = null
    }

    override fun close() {
        stop()
        connectedListener?.let { connectedRef?.removeEventListener(it) }
        connectedListener = null
    }

    override fun push(updates: Map<String, Any?>) {
        if (updates.isNotEmpty()) ref?.updateChildren(updates)
    }

    override fun create(code: String, data: AppData, onError: (String) -> Unit) {
        family(code).setValue(SyncCodec.full(data)).addOnFailureListener { onError(it.message ?: "Błąd zapisu") }
    }

    /** Pobiera dane rodziny z serwera. null w onResult = nie ma takiej rodziny. */
    override fun fetch(code: String, onResult: (AppData?) -> Unit, onError: (String) -> Unit) {
        family(code).get()
            .addOnSuccessListener { onResult(decode(it)) }
            .addOnFailureListener { onError(it.message ?: "Brak połączenia") }
    }

    private fun decode(s: DataSnapshot): AppData? = SyncCodec.decode(
        configJson = s.child("config").getValue(String::class.java),
        events = s.child("events").children.mapNotNull { it.getValue(String::class.java) },
        earned = s.child("earned").children.mapNotNull { c -> c.getValue(Long::class.java)?.let { c.key!! to it } }.toMap(),
        missions = s.child("missions").children.mapNotNull { it.getValue(String::class.java) },
    )
}
