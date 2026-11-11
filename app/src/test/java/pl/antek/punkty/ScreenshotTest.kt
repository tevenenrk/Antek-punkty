package pl.antek.punkty

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.Defaults
import pl.antek.punkty.data.EarnedBadge
import pl.antek.punkty.data.EventKind
import pl.antek.punkty.data.PointEvent
import pl.antek.punkty.ui.ForestApp
import pl.antek.punkty.ui.theme.ForestTheme
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun seed() {
        val ctx = ApplicationProvider.getApplicationContext<android.app.Application>()
        val base = Defaults.create()
        val now = System.currentTimeMillis()
        val day = 86_400_000L
        val events = mutableListOf<PointEvent>()
        var i = 0
        fun add(id: String, ago: Long) {
            val b = base.behaviors.first { it.id == id }
            events += PointEvent("e${i++}", now - ago, EventKind.BEHAVIOR, b.id, b.name, b.icon, b.signedPoints)
        }
        for (d in 0..5) { add("b_teeth", d * day + 1000); add("b_bed", d * day + 2000); add("b_reading", d * day + 3000) }
        add("b_dress", 4000); add("b_homework", 5000); add("n_anger", 6000)
        val room = base.missions.first()
        val data: AppData = base.copy(
            missions = listOf(room.copy(steps = room.steps.mapIndexed { idx, st -> st.copy(done = idx < 2) })),
            events = events,
            earned = listOf(EarnedBadge("g_morning", now)),
            createdAt = now - 10 * day,
        )
        File(ctx.filesDir, "data.json").writeText(Json.encodeToString(AppData.serializer(), data))
    }

    private fun shot(name: String) {
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
        val dir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test
    fun screens() {
        seed()
        rule.mainClock.autoAdvance = false
        rule.setContent { ForestTheme { ForestApp() } }
        rule.mainClock.advanceTimeBy(3000)
        shot("1_las")
        rule.onNodeWithText("Zachowania").performClick(); rule.mainClock.advanceTimeBy(1500)
        shot("2_zachowania")
        rule.onNodeWithText("Misje").performClick(); rule.mainClock.advanceTimeBy(1500)
        shot("6_misje")
        rule.onNodeWithText("Nagrody").performClick(); rule.mainClock.advanceTimeBy(1500)
        shot("3_nagrody")
        rule.onNodeWithText("Odznaki").performClick(); rule.mainClock.advanceTimeBy(1500)
        shot("4_odznaki")
        rule.onNodeWithText("Więcej").performClick(); rule.mainClock.advanceTimeBy(1500)
        shot("5_wiecej")
    }

    @Test
    fun badgeCreatorCreatesBadge() {
        seed()
        rule.mainClock.autoAdvance = false
        rule.setContent { ForestTheme { ForestApp() } }
        rule.mainClock.advanceTimeBy(1000)
        rule.onNodeWithText("Odznaki").performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("✨ Kreator").performClick(); rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Nazwa odznaki, np. „Poranny Bohater”").performTextInput("Zębowy Smok")
        rule.onNodeWithText("Seria dni z rzędu").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        rule.mainClock.advanceTimeBy(300)
        rule.onNodeWithText("Ścielenie łóżka").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        rule.mainClock.advanceTimeBy(500)
        rule.onNodeWithText("Zapisz").performClick(); rule.mainClock.advanceTimeBy(500)
        val vm = androidx.lifecycle.ViewModelProvider(rule.activity)[pl.antek.punkty.ui.AppViewModel::class.java]
        val b = vm.data.value.badges.single { it.name == "Zębowy Smok" }
        org.junit.Assert.assertEquals(pl.antek.punkty.data.BadgeType.STREAK, b.type)
        org.junit.Assert.assertEquals(listOf("b_bed"), b.items.map { it.behaviorId })
        org.junit.Assert.assertEquals(3, b.target)
    }
}
