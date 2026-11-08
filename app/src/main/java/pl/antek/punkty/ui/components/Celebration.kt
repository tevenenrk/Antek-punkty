package pl.antek.punkty.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import pl.antek.punkty.data.Behavior
import pl.antek.punkty.logic.BadgeEngine
import pl.antek.punkty.logic.pointsWord
import pl.antek.punkty.ui.Celebration
import pl.antek.punkty.ui.theme.Forest
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun CelebrationDialog(c: Celebration, behaviors: List<Behavior>, onDismiss: () -> Unit) {
    val icon: String
    val title: String
    val text: String
    when (c) {
        is Celebration.BadgeEarned -> {
            icon = c.badge.icon
            title = "Nowa odznaka!"
            text = buildString {
                append(c.badge.name)
                append("\n")
                append(BadgeEngine.describe(c.badge, behaviors))
                if (c.badge.bonus > 0) append("\n\nBonus: +${c.badge.bonus} ${pointsWord(c.badge.bonus)} 🌰")
            }
        }
        is Celebration.LevelUp -> {
            icon = c.level.icon
            title = "Nowy poziom!"
            text = "Poziom ${c.level.number}\nJesteś teraz: ${c.level.name}"
        }
        is Celebration.RewardRedeemed -> {
            icon = c.reward.icon
            title = "Nagroda odebrana!"
            text = "${c.reward.name}\nCiesz się nagrodą! 🎉"
        }
    }
    Dialog(onDismissRequest = onDismiss) {
        Box(contentAlignment = Alignment.Center) {
            ParchmentCard(Modifier.fillMaxWidth(), padding = 24.dp) {
                val scale = remember { Animatable(0.2f) }
                LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 200f)) }
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = MaterialTheme.typography.headlineMedium, color = Forest.Deep, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier
                            .size(140.dp)
                            .graphicsLayer { scaleX = scale.value; scaleY = scale.value }
                            .clip(CircleShape)
                            .background(Brush.radialGradient(listOf(Forest.Gold.copy(alpha = 0.7f), Color.Transparent))),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(icon, fontSize = 72.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(text, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium, color = Forest.BarkDark)
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Forest.Moss),
                    ) {
                        Text("Super! 🎉", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            FallingLeaves(Modifier.matchParentSize())
        }
    }
}

private data class Leaf(val x: Float, val offset: Float, val speed: Int, val rot: Float, val color: Color)

@Composable
fun FallingLeaves(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "leaves")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5000, easing = LinearEasing)), label = "p")
    val colors = listOf(Color(0xFF8BC34A), Color(0xFFF2B705), Color(0xFFE57373), Color(0xFFFFB74D), Color(0xFF4F7942))
    val leaves = remember {
        val r = Random(7)
        List(26) { Leaf(r.nextFloat(), r.nextFloat(), 1 + r.nextInt(2), r.nextFloat() * 360f, colors[r.nextInt(colors.size)]) }
    }
    Canvas(modifier) {
        val lw = 9.dp.toPx()
        leaves.forEach { l ->
            val y = ((l.offset + p * l.speed) % 1f) * size.height
            val x = l.x * size.width + sin((p * 2 * PI * l.speed + l.offset * 6).toFloat()) * 12.dp.toPx()
            rotate(l.rot + p * 360f * l.speed, pivot = Offset(x, y)) {
                drawOval(l.color.copy(alpha = 0.85f), topLeft = Offset(x - lw, y - lw / 2), size = Size(lw * 2, lw))
            }
        }
    }
}
