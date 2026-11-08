package pl.antek.punkty.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.random.Random

/** Bajkowy las w tle: niebo, słońce, wzgórza, choinki, muchomorki i migające świetliki. */
@Composable
fun ForestBackground(content: @Composable () -> Unit) {
    val t = rememberInfiniteTransition(label = "fireflies")
    val glow by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart),
        label = "glow",
    )
    val fireflies = remember {
        val r = Random(42)
        List(16) { Triple(r.nextFloat(), 0.15f + r.nextFloat() * 0.7f, r.nextFloat()) }
    }
    Box(
        Modifier
            .fillMaxSize()
            .drawWithCache {
                val sky = Brush.verticalGradient(
                    listOf(Color(0xFFB9E3EE), Color(0xFFE6F4D7), Color(0xFFD7EBC0)),
                )
                onDrawBehind { drawForest(sky) }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            fireflies.forEach { (x, y, phase) ->
                val p = (glow + phase) % 1f
                val a = if (p < 0.5f) p * 2f else (1f - p) * 2f
                val c = Offset(x * size.width, y * size.height + (p - 0.5f) * 30f)
                drawCircle(Color(0xFFFFF59D).copy(alpha = 0.35f * a), radius = 14f, center = c)
                drawCircle(Color(0xFFFFEB3B).copy(alpha = 0.9f * a), radius = 4f, center = c)
            }
        }
        content()
    }
}

private fun DrawScope.drawForest(sky: Brush) {
    val w = size.width
    val h = size.height
    drawRect(sky)

    // słońce
    val sun = Offset(w * 0.84f, h * 0.09f)
    drawCircle(Color(0x40FFF3B0), radius = w * 0.26f, center = sun)
    drawCircle(Color(0x66FFE9A0), radius = w * 0.16f, center = sun)
    drawCircle(Color(0xFFFFE38A), radius = w * 0.1f, center = sun)

    // chmurki
    cloud(w * 0.18f, h * 0.07f, w * 0.09f)
    cloud(w * 0.55f, h * 0.15f, w * 0.06f)

    // dalekie wzgórza
    val hill1 = Path().apply {
        moveTo(0f, h * 0.6f)
        quadraticTo(w * 0.25f, h * 0.48f, w * 0.5f, h * 0.58f)
        quadraticTo(w * 0.75f, h * 0.67f, w, h * 0.54f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(hill1, Color(0xFFB5DDA4))

    // dalekie drzewa
    for (i in 0..11) {
        val x = w * (i / 11f)
        val hh = h * (0.10f + (i % 3) * 0.025f)
        pine(x, h * 0.66f + (i % 2) * h * 0.015f, hh, Color(0xFF8CC084))
    }

    // bliższe wzgórze
    val hill2 = Path().apply {
        moveTo(0f, h * 0.76f)
        quadraticTo(w * 0.35f, h * 0.68f, w * 0.65f, h * 0.76f)
        quadraticTo(w * 0.85f, h * 0.81f, w, h * 0.73f)
        lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(hill2, Color(0xFF9CCC65))

    // duże drzewa po bokach
    pine(w * 0.02f, h * 0.93f, h * 0.34f, Color(0xFF2E7D32))
    pine(w * 0.14f, h * 0.95f, h * 0.24f, Color(0xFF388E3C))
    pine(w * 0.88f, h * 0.95f, h * 0.27f, Color(0xFF388E3C))
    pine(w * 1.0f, h * 0.93f, h * 0.36f, Color(0xFF2E7D32))

    // trawa
    drawRect(Color(0xFF7CB342), topLeft = Offset(0f, h * 0.92f), size = Size(w, h * 0.08f))

    // muchomorki
    mushroom(w * 0.27f, h * 0.935f, w * 0.035f)
    mushroom(w * 0.33f, h * 0.94f, w * 0.022f)
    mushroom(w * 0.74f, h * 0.937f, w * 0.03f)
}

private fun DrawScope.cloud(cx: Float, cy: Float, r: Float) {
    val c = Color.White.copy(alpha = 0.85f)
    drawCircle(c, r, Offset(cx, cy))
    drawCircle(c, r * 0.75f, Offset(cx - r * 0.95f, cy + r * 0.2f))
    drawCircle(c, r * 0.8f, Offset(cx + r * 1.0f, cy + r * 0.15f))
}

private fun DrawScope.pine(cx: Float, baseY: Float, height: Float, color: Color) {
    val trunkW = height * 0.08f
    drawRect(
        Color(0xFF5D4037),
        topLeft = Offset(cx - trunkW / 2, baseY - height * 0.16f),
        size = Size(trunkW, height * 0.16f),
    )
    for (i in 0 until 3) {
        val top = baseY - height + i * height * 0.22f
        val bottom = top + height * 0.42f
        val half = height * (0.17f + 0.07f * i)
        val p = Path().apply {
            moveTo(cx, top)
            lineTo(cx + half, bottom)
            lineTo(cx - half, bottom)
            close()
        }
        drawPath(p, color)
    }
}

private fun DrawScope.mushroom(cx: Float, baseY: Float, r: Float) {
    drawRect(
        Color(0xFFFFF3E0),
        topLeft = Offset(cx - r * 0.3f, baseY - r * 1.1f),
        size = Size(r * 0.6f, r * 1.1f),
    )
    drawArc(
        Color(0xFFD84343),
        startAngle = 180f,
        sweepAngle = 180f,
        useCenter = true,
        topLeft = Offset(cx - r, baseY - r * 1.9f),
        size = Size(r * 2f, r * 1.7f),
    )
    drawCircle(Color.White, r * 0.18f, Offset(cx - r * 0.4f, baseY - r * 1.35f))
    drawCircle(Color.White, r * 0.14f, Offset(cx + r * 0.35f, baseY - r * 1.5f))
    drawCircle(Color.White, r * 0.1f, Offset(cx + r * 0.05f, baseY - r * 1.7f))
}
