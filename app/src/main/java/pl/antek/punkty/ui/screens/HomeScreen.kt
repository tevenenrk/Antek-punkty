package pl.antek.punkty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.Badge
import pl.antek.punkty.data.EventKind
import pl.antek.punkty.logic.BadgeEngine
import pl.antek.punkty.logic.Levels
import pl.antek.punkty.logic.pointsWord
import pl.antek.punkty.ui.components.EventRow
import pl.antek.punkty.ui.components.ForestProgress
import pl.antek.punkty.ui.components.Medallion
import pl.antek.punkty.ui.components.ParchmentCard
import pl.antek.punkty.ui.components.PointsPill
import pl.antek.punkty.ui.components.SectionTitle
import pl.antek.punkty.ui.components.WoodenSign
import pl.antek.punkty.ui.theme.Forest

@Composable
fun HomeScreen(data: AppData, onGoToBehaviors: () -> Unit) {
    val todayEvents = remember(data.events) {
        data.events.filter { BadgeEngine.isToday(it.time) }.sortedByDescending { it.time }
    }
    val earned: List<Badge> = remember(data.earned, data.badges) {
        data.earned.sortedByDescending { it.time }.mapNotNull { e -> data.badges.find { it.id == e.badgeId } }
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { HeroCard(data) }
        item { OwlTip(mascotMessage(data, todayEvents.isNotEmpty())) }
        item {
            ParchmentCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle("🌞 Dzisiaj w lesie", Modifier.weight(1f))
                    val plus = todayEvents.filter { it.kind == EventKind.BEHAVIOR && it.points > 0 }.sumOf { it.points }
                    val minus = todayEvents.filter { it.kind == EventKind.BEHAVIOR && it.points < 0 }.sumOf { it.points }
                    if (todayEvents.isNotEmpty()) {
                        Text("+$plus", color = Forest.Moss, fontWeight = FontWeight.ExtraBold)
                        if (minus < 0) {
                            Spacer(Modifier.width(8.dp))
                            Text("−${-minus}", color = Forest.Mushroom, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
                Spacer(Modifier.height(6.dp))
                if (todayEvents.isEmpty()) {
                    Text("Jeszcze nic się dziś nie wydarzyło. Do dzieła! 🐾", color = Forest.BarkDark)
                    Spacer(Modifier.height(10.dp))
                    Button(onClick = onGoToBehaviors, colors = ButtonDefaults.buttonColors(containerColor = Forest.Moss)) {
                        Text("⭐ Przyznaj punkty")
                    }
                } else {
                    todayEvents.take(10).forEach { EventRow(it, showDate = false) }
                    if (todayEvents.size > 10) {
                        Text("…i jeszcze ${todayEvents.size - 10}", color = Forest.BarkLight)
                    }
                }
            }
        }
        item {
            ParchmentCard(Modifier.fillMaxWidth()) {
                SectionTitle("🏅 Odznaki: ${earned.size} / ${data.badges.size}")
                Spacer(Modifier.height(10.dp))
                if (earned.isEmpty()) {
                    Text("Jeszcze żadnej – odkryj je w zakładce Odznaki!", color = Forest.BarkDark)
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(earned, key = { it.id }) { b ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(84.dp)) {
                                Medallion(b.icon, earned = true, size = 64.dp)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    b.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroCard(data: AppData) {
    val xp = data.xp
    val level = Levels.forXp(xp)
    val next = Levels.next(level)
    WoodenSign(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Brush.radialGradient(listOf(Color(0xFFF1F8E9), Forest.Leaf)))
                    .border(4.dp, Forest.Gold, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(level.icon, fontSize = 46.sp)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(data.childName, style = MaterialTheme.typography.headlineSmall, color = Forest.Parchment)
                Text("Poziom ${level.number} · ${level.name}", style = MaterialTheme.typography.titleSmall, color = Forest.Gold, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                ForestProgress(Levels.progress(xp), color = Forest.Gold, trackColor = Color.White.copy(alpha = 0.25f))
                Spacer(Modifier.height(4.dp))
                Text(
                    if (next != null) "⭐ $xp / ${next.minXp} – dalej: ${next.icon} ${next.name}" else "⭐ $xp – najwyższy poziom!",
                    style = MaterialTheme.typography.bodySmall,
                    color = Forest.Parchment.copy(alpha = 0.9f),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Skarbonka z żołędziami", color = Forest.Parchment, fontWeight = FontWeight.Bold)
                Text("Punkty do wydania na nagrody", color = Forest.Parchment.copy(alpha = 0.8f), style = MaterialTheme.typography.bodySmall)
            }
            PointsPill(data.balance, big = true)
        }
    }
}

@Composable
private fun OwlTip(message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text("🦉", fontSize = 48.sp)
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 20.dp),
            color = Color.White.copy(alpha = 0.92f),
            shadowElevation = 2.dp,
        ) {
            Text(message, Modifier.padding(14.dp), color = Forest.Deep, fontWeight = FontWeight.Medium)
        }
    }
}

private fun mascotMessage(data: AppData, anyToday: Boolean): String {
    val balance = data.balance
    val affordable = data.rewards.filter { it.cost <= balance }.maxByOrNull { it.cost }
    val nearest = data.rewards.filter { it.cost > balance }.minByOrNull { it.cost }
    return when {
        affordable != null && anyToday ->
            "Hu-hu! Masz już dość punktów na ${affordable.icon} ${affordable.name}! Zapytaj rodzica 🎉"
        !anyToday -> "Hu-hu! Nowy dzień w lesie. Co dobrego dziś zrobimy?"
        nearest != null -> {
            val missing = nearest.cost - balance
            "Hu-hu! Do nagrody ${nearest.icon} ${nearest.name} brakuje jeszcze $missing ${pointsWord(missing)}!"
        }
        else -> "Hu-hu! Świetnie Ci idzie, leśny przyjacielu!"
    }
}
