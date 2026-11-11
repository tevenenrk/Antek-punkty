package pl.antek.punkty.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private val Chalk = Color(0xFFF5F5EB)

/**
 * Leśna scena: pluszowy miś w hełmie i mundurze moro wskazuje patykiem na tablicę,
 * na której kredą wypisana jest misja.
 */
@Composable
fun MissionBoardScene(
    title: String?,
    done: Int,
    total: Int,
    reward: Int?,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(26.dp)),
    ) {
        val w = maxWidth
        val h = maxHeight
        Canvas(Modifier.fillMaxSize()) { drawScene() }
        Column(
            Modifier
                .offset(x = w * 0.47f, y = h * 0.095f)
                .width(w * 0.46f)
                .height(h * 0.54f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "★ MISJA ★",
                color = Chalk.copy(alpha = 0.75f),
                fontSize = 11.sp,
                letterSpacing = 3.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                title ?: "Czekam na rozkazy!",
                color = Chalk,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (title != null && total > 0) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "✔ $done / $total" + (reward?.takeIf { it > 0 }?.let { "   🌰 $it" } ?: ""),
                    color = Chalk.copy(alpha = 0.9f),
                    fontFamily = FontFamily.Cursive,
                    fontSize = 16.sp,
                )
            } else if (title == null) {
                Text(
                    "Nowa misja w kreatorze",
                    color = Chalk.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Cursive,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

private fun DrawScope.drawScene() {
    val w = size.width
    val h = size.height

    // niebo i las w tle
    drawRect(Brush.verticalGradient(listOf(Color(0xFFBFE6F0), Color(0xFFE6F4D2))))
    drawCircle(Color(0x55FFF3B0), h * 0.3f, Offset(w * 0.08f, h * 0.1f))
    for (i in 0..7) {
        pine(w * (0.02f + i * 0.14f), h * 0.84f, h * (0.38f + (i % 3) * 0.08f), Color(0xFFA9D8A0))
    }
    for (i in 0..3) {
        pine(w * (0.1f + i * 0.3f), h * 0.88f, h * (0.28f + (i % 2) * 0.06f), Color(0xFF81C784))
    }

    // trawa
    drawRect(Color(0xFF8BC34A), Offset(0f, h * 0.84f), Size(w, h * 0.16f))
    drawOval(Color(0xFF7CB342), Offset(-w * 0.1f, h * 0.8f), Size(w * 0.65f, h * 0.12f))

    // tablica
    val wood = Color(0xFF8D6E63)
    val woodDark = Color(0xFF5D4037)
    val bl = w * 0.44f
    val br = w * 0.96f
    val bt = h * 0.06f
    val bb = h * 0.68f
    drawRect(woodDark, Offset(bl + w * 0.06f, bb), Size(w * 0.025f, h * 0.88f - bb))
    drawRect(woodDark, Offset(br - w * 0.085f, bb), Size(w * 0.025f, h * 0.88f - bb))
    drawRoundRect(wood, Offset(bl, bt), Size(br - bl, bb - bt), CornerRadius(h * 0.04f))
    drawRoundRect(woodDark, Offset(bl, bt), Size(br - bl, bb - bt), CornerRadius(h * 0.04f), style = Stroke(h * 0.012f))
    val pad = h * 0.035f
    drawRoundRect(
        Color(0xFF2E4A36),
        Offset(bl + pad, bt + pad),
        Size(br - bl - 2 * pad, bb - bt - 2 * pad),
        CornerRadius(h * 0.02f),
    )
    // smugi kredy
    drawOval(Color.White.copy(alpha = 0.06f), Offset(bl + w * 0.06f, bt + h * 0.38f), Size(w * 0.2f, h * 0.08f))
    drawOval(Color.White.copy(alpha = 0.05f), Offset(bl + w * 0.28f, bt + h * 0.06f), Size(w * 0.16f, h * 0.06f))
    // półka na kredę
    drawRoundRect(woodDark, Offset(bl + w * 0.05f, bb - h * 0.01f), Size(br - bl - w * 0.1f, h * 0.03f), CornerRadius(h * 0.01f))
    drawRoundRect(Chalk, Offset(br - w * 0.16f, bb - h * 0.03f), Size(w * 0.04f, h * 0.02f), CornerRadius(h * 0.01f))

    drawBear(w * 0.2f, w, h)
}

private fun DrawScope.drawBear(cx: Float, w: Float, h: Float) {
    val brown = Color(0xFFA0703F)
    val brownLight = Color(0xFFDDB27C)
    val dark = Color(0xFF3E2723)
    val olive = Color(0xFF5E6E30)
    val oliveDark = Color(0xFF3F4B1C)
    val camoSand = Color(0xFF8A7A45)
    val gold = Color(0xFFF2B705)
    val ground = h * 0.92f

    // nogi i buty
    drawOval(olive, Offset(cx - h * 0.16f, ground - h * 0.18f), Size(h * 0.14f, h * 0.17f))
    drawOval(olive, Offset(cx + h * 0.02f, ground - h * 0.18f), Size(h * 0.14f, h * 0.17f))
    drawOval(dark, Offset(cx - h * 0.19f, ground - h * 0.055f), Size(h * 0.17f, h * 0.075f))
    drawOval(dark, Offset(cx + h * 0.02f, ground - h * 0.055f), Size(h * 0.17f, h * 0.075f))

    // lewa ręka (opuszczona)
    drawOval(olive, Offset(cx - h * 0.23f, h * 0.5f), Size(h * 0.09f, h * 0.2f))
    drawCircle(brown, h * 0.045f, Offset(cx - h * 0.185f, h * 0.7f))

    // tułów w mundurze moro
    val body = Rect(cx - h * 0.17f, h * 0.44f, cx + h * 0.17f, ground - h * 0.1f)
    val bodyPath = Path().apply { addOval(body) }
    drawPath(bodyPath, olive)
    clipPath(bodyPath) {
        drawOval(oliveDark, Offset(cx - h * 0.16f, h * 0.48f), Size(h * 0.12f, h * 0.07f))
        drawOval(camoSand, Offset(cx + h * 0.02f, h * 0.5f), Size(h * 0.13f, h * 0.06f))
        drawOval(oliveDark, Offset(cx + h * 0.05f, h * 0.62f), Size(h * 0.12f, h * 0.08f))
        drawOval(camoSand, Offset(cx - h * 0.14f, h * 0.63f), Size(h * 0.1f, h * 0.05f))
        drawOval(oliveDark, Offset(cx - h * 0.05f, h * 0.74f), Size(h * 0.12f, h * 0.06f))
        // pas
        drawRect(Color(0xFF4E342E), Offset(body.left, h * 0.69f), Size(body.width, h * 0.035f))
        drawRect(gold, Offset(cx - h * 0.025f, h * 0.688f), Size(h * 0.05f, h * 0.04f))
    }
    star(Offset(cx - h * 0.075f, h * 0.56f), h * 0.035f, gold)

    // prawa ręka wskazuje tablicę
    val shoulder = Offset(cx + h * 0.12f, h * 0.53f)
    val paw = Offset(cx + h * 0.33f, h * 0.4f)
    drawLine(olive, shoulder, paw, strokeWidth = h * 0.085f, cap = StrokeCap.Round)
    // wskaźnik
    val tip = Offset(w * 0.5f, h * 0.24f)
    drawLine(Color(0xFF8D6E63), paw, tip, strokeWidth = h * 0.016f, cap = StrokeCap.Round)
    drawCircle(Color(0xFFD84343), h * 0.014f, tip)
    drawCircle(brown, h * 0.048f, paw)

    // głowa
    val hy = h * 0.3f
    val r = h * 0.14f
    drawCircle(brown, r * 0.36f, Offset(cx - r * 0.98f, hy - r * 0.28f))
    drawCircle(brownLight, r * 0.18f, Offset(cx - r * 0.98f, hy - r * 0.28f))
    drawCircle(brown, r * 0.36f, Offset(cx + r * 0.98f, hy - r * 0.28f))
    drawCircle(brownLight, r * 0.18f, Offset(cx + r * 0.98f, hy - r * 0.28f))
    drawCircle(brown, r, Offset(cx, hy))
    // pyszczek
    drawOval(brownLight, Offset(cx - r * 0.45f, hy + r * 0.08f), Size(r * 0.9f, r * 0.65f))
    drawOval(dark, Offset(cx - r * 0.16f, hy + r * 0.13f), Size(r * 0.32f, r * 0.21f))
    drawArc(
        dark, 20f, 140f, false,
        Offset(cx - r * 0.22f, hy + r * 0.2f), Size(r * 0.44f, r * 0.34f),
        style = Stroke(r * 0.07f, cap = StrokeCap.Round),
    )
    // oczy i rumieńce
    for (side in listOf(-1f, 1f)) {
        val eye = Offset(cx + side * r * 0.4f, hy - r * 0.08f)
        drawCircle(dark, r * 0.11f, eye)
        drawCircle(Color.White, r * 0.04f, eye + Offset(r * 0.03f, -r * 0.04f))
        drawCircle(Color(0x66FF8A80), r * 0.14f, Offset(cx + side * r * 0.66f, hy + r * 0.24f))
    }
    // hełm moro
    val helmetTopLeft = Offset(cx - r * 1.12f, hy - r * 1.22f)
    val helmetSize = Size(r * 2.24f, r * 1.7f)
    val helmet = Path().apply { addArc(Rect(helmetTopLeft, helmetSize), 180f, 180f); close() }
    drawPath(helmet, olive)
    clipPath(helmet) {
        drawOval(oliveDark, Offset(cx - r * 0.75f, hy - r * 0.9f), Size(r * 0.45f, r * 0.25f))
        drawOval(camoSand, Offset(cx + r * 0.1f, hy - r * 1.1f), Size(r * 0.5f, r * 0.25f))
        drawOval(oliveDark, Offset(cx + r * 0.45f, hy - r * 0.7f), Size(r * 0.45f, r * 0.25f))
    }
    drawRoundRect(oliveDark, Offset(cx - r * 1.25f, hy - r * 0.44f), Size(r * 2.5f, r * 0.17f), CornerRadius(r * 0.09f))
    star(Offset(cx, hy - r * 0.75f), r * 0.18f, gold)
}

private fun DrawScope.star(c: Offset, r: Float, color: Color) {
    val p = Path()
    for (i in 0 until 10) {
        val a = -PI / 2 + i * PI / 5
        val rr = if (i % 2 == 0) r else r * 0.45f
        val x = c.x + (cos(a) * rr).toFloat()
        val y = c.y + (sin(a) * rr).toFloat()
        if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
    }
    p.close()
    drawPath(p, color)
}
