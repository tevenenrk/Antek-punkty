package pl.antek.punkty.data

import kotlinx.serialization.json.Json
import java.io.File

class Repository(private val dir: File) {
    private val file = File(dir, "data.json")
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun load(): AppData {
        if (!file.exists()) return Defaults.create()
        return try {
            json.decodeFromString(AppData.serializer(), file.readText())
        } catch (e: Exception) {
            // Nie gubimy danych: odkładamy uszkodzony plik na bok.
            file.renameTo(File(dir, "data.broken.${System.currentTimeMillis()}.json"))
            Defaults.create()
        }
    }

    fun save(data: AppData) {
        val tmp = File(dir, "data.json.tmp")
        tmp.writeText(json.encodeToString(AppData.serializer(), data))
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }
}
