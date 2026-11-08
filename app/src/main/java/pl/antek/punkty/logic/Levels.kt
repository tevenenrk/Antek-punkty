package pl.antek.punkty.logic

data class Level(val number: Int, val name: String, val icon: String, val minXp: Int)

object Levels {
    val all = listOf(
        Level(1, "Nasionko", "🌱", 0),
        Level(2, "Kiełek", "🌿", 30),
        Level(3, "Listek Koniczyny", "🍀", 80),
        Level(4, "Leśny Grzybek", "🍄", 150),
        Level(5, "Zwinna Wiewiórka", "🐿️", 250),
        Level(6, "Dzielny Jeż", "🦔", 400),
        Level(7, "Sprytny Lis", "🦊", 600),
        Level(8, "Mądra Sowa", "🦉", 850),
        Level(9, "Szlachetny Jeleń", "🦌", 1150),
        Level(10, "Stary Dąb", "🌳", 1500),
        Level(11, "Leśny Czarodziej", "🧚", 2000),
        Level(12, "Król Lasu", "👑", 3000),
    )

    fun forXp(xp: Int): Level = all.last { xp >= it.minXp }

    fun next(level: Level): Level? = all.getOrNull(level.number)

    fun progress(xp: Int): Float {
        val level = forXp(xp)
        val next = next(level) ?: return 1f
        return ((xp - level.minXp).toFloat() / (next.minXp - level.minXp)).coerceIn(0f, 1f)
    }
}
