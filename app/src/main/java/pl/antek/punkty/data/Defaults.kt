package pl.antek.punkty.data

object Defaults {
    fun create(): AppData {
        val now = System.currentTimeMillis()
        val behaviors = listOf(
            Behavior("b_teeth", "Mycie zębów", "🦷", 2, true),
            Behavior("b_bed", "Ścielenie łóżka", "🛏️", 2, true),
            Behavior("b_dress", "Ubranie się samemu", "👕", 2, true),
            Behavior("b_homework", "Odrabianie lekcji", "📚", 5, true),
            Behavior("b_toys", "Sprzątanie zabawek", "🧸", 3, true),
            Behavior("b_veggies", "Zjedzenie warzyw", "🥦", 2, true),
            Behavior("b_reading", "Czytanie książki", "📖", 3, true),
            Behavior("b_help", "Pomoc w domu", "🤝", 4, true),
            Behavior("b_polite", "Grzeczne zachowanie", "😊", 3, true),
            Behavior("b_sleep", "Spanie o czasie", "😴", 2, true),
            Behavior("n_anger", "Złość i krzyk", "😡", 3, false),
            Behavior("n_hit", "Bicie", "👊", 5, false),
            Behavior("n_lie", "Kłamstwo", "🤥", 4, false),
            Behavior("n_ignore", "Nieposłuszeństwo", "🙉", 2, false),
            Behavior("n_screen", "Za długo z telefonem", "📱", 2, false),
            Behavior("n_mess", "Zostawiony bałagan", "🌪️", 2, false),
        )
        val rewards = listOf(
            Reward("r_cartoon", "Bajka (30 min)", "📺", 15),
            Reward("r_icecream", "Lody", "🍦", 20),
            Reward("r_game", "Gra (30 min)", "🎮", 25),
            Reward("r_playground", "Wyjście na plac zabaw", "🎠", 30),
            Reward("r_pizza", "Pizza na obiad", "🍕", 80),
            Reward("r_cinema", "Wyjście do kina", "🎬", 120),
            Reward("r_toy", "Nowa zabawka", "🧸", 200),
        )
        val badges = listOf(
            Badge(
                "g_morning", "Poranny Bohater", "☀️", BadgeType.COMBO_DAY,
                listOf(BadgeItem("b_teeth"), BadgeItem("b_bed"), BadgeItem("b_dress")),
                target = 1, bonus = 5, createdAt = now,
            ),
            Badge(
                "g_smile", "Lśniący Uśmiech", "🦊", BadgeType.STREAK,
                listOf(BadgeItem("b_teeth", 2)),
                target = 7, bonus = 10, createdAt = now,
            ),
            Badge(
                "g_owl", "Mądra Sówka", "🦉", BadgeType.TOTAL,
                listOf(BadgeItem("b_reading")),
                target = 10, bonus = 15, createdAt = now,
            ),
            Badge(
                "g_hedgehog", "Spokojny Jeżyk", "🦔", BadgeType.ABSTAIN,
                listOf(BadgeItem("n_anger"), BadgeItem("n_hit")),
                target = 7, bonus = 15, createdAt = now,
            ),
            Badge(
                "g_squirrel", "Pracowita Wiewiórka", "🐿️", BadgeType.TOTAL,
                listOf(BadgeItem("b_help"), BadgeItem("b_toys")),
                target = 20, bonus = 20, createdAt = now,
            ),
        )
        val missions = listOf(
            Mission(
                id = "m_room",
                title = "Operacja Czysty Pokój",
                icon = "🧹",
                description = "Tajna misja: przywrócić porządek w bazie!",
                steps = listOf(
                    MissionStep("m_room_1", "Zbierz klocki do pudełka"),
                    MissionStep("m_room_2", "Odłóż książki na półkę"),
                    MissionStep("m_room_3", "Wrzuć brudne ubrania do kosza"),
                    MissionStep("m_room_4", "Pościel łóżko"),
                ),
                reward = 15,
                createdAt = now,
            ),
        )
        return AppData(
            childName = "Antek",
            missions = missions,
            behaviors = behaviors,
            rewards = rewards,
            badges = badges,
            createdAt = now,
        )
    }
}
