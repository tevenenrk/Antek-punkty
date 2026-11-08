package pl.antek.punkty.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.Badge
import pl.antek.punkty.data.BadgeItem
import pl.antek.punkty.data.BadgeType
import pl.antek.punkty.data.Behavior
import pl.antek.punkty.data.EarnedBadge
import pl.antek.punkty.data.IconSets
import pl.antek.punkty.data.newId
import pl.antek.punkty.logic.BadgeEngine
import pl.antek.punkty.logic.BadgeProgress
import pl.antek.punkty.logic.emoji
import pl.antek.punkty.logic.explanation
import pl.antek.punkty.logic.pointsWord
import pl.antek.punkty.logic.title
import pl.antek.punkty.ui.AppViewModel
import pl.antek.punkty.ui.components.*
import pl.antek.punkty.ui.theme.Forest

@Composable
fun BadgesScreen(data: AppData, vm: AppViewModel, requireParent: (() -> Unit) -> Unit) {
    var creating by remember { mutableStateOf<Badge?>(null) }
    var details by remember { mutableStateOf<Badge?>(null) }
    val progress = remember(data) { data.badges.associate { it.id to BadgeEngine.progress(it, data) } }
    val earnedMap = data.earned.associateBy { it.badgeId }
    val sorted = data.badges.sortedWith(
        compareByDescending<Badge> { it.id in earnedMap }.thenByDescending { progress[it.id]?.fraction ?: 0f },
    )

    LazyVerticalGrid(
        columns = GridCells.Adaptive(156.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ParchmentCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("🏅 Leśne odznaki")
                        Text("Zdobyte: ${earnedMap.size} z ${data.badges.size}", color = Forest.BarkDark)
                    }
                    Button(
                        onClick = {
                            requireParent {
                                creating = Badge(newId(), "", "🏅", BadgeType.COMBO_DAY, emptyList(), target = 3, bonus = 5)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Forest.Moss),
                    ) { Text("✨ Kreator") }
                }
            }
        }
        items(sorted, key = { it.id }) { b ->
            BadgeCard(b, earnedMap[b.id], progress.getValue(b.id), BadgeEngine.describe(b, data.behaviors)) { details = b }
        }
        if (data.badges.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text("Nie ma jeszcze odznak. Stwórz pierwszą w kreatorze!", color = Forest.BarkDark)
            }
        }
    }

    details?.let { b ->
        val e = earnedMap[b.id]
        val p = progress[b.id] ?: BadgeProgress(0, 1)
        AlertDialog(
            onDismissRequest = { details = null },
            containerColor = Forest.Parchment,
            icon = { Medallion(b.icon, earned = e != null, size = 96.dp) },
            title = { Text(b.name, textAlign = TextAlign.Center) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                    Text("${b.type.emoji} ${b.type.title}", fontWeight = FontWeight.Bold, color = Forest.Moss)
                    Spacer(Modifier.height(6.dp))
                    Text(BadgeEngine.describe(b, data.behaviors), textAlign = TextAlign.Center)
                    if (b.bonus > 0) {
                        Spacer(Modifier.height(6.dp))
                        Text("Nagroda: +${b.bonus} ${pointsWord(b.bonus)} 🌰", color = Forest.Bark, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(12.dp))
                    if (e != null) {
                        Text("✅ Zdobyta ${formatDate(e.time)}", fontWeight = FontWeight.Bold, color = Forest.Deep)
                    } else {
                        ForestProgress(p.fraction)
                        Spacer(Modifier.height(4.dp))
                        Text("${p.current} / ${p.target} ${BadgeEngine.progressUnit(b.type, p.target)}")
                    }
                }
            },
            confirmButton = { Button(onClick = { details = null }) { Text("OK") } },
            dismissButton = {
                TextButton(onClick = {
                    details = null
                    requireParent { creating = b }
                }) { Text("✏️ Edytuj") }
            },
        )
    }

    creating?.let { b ->
        BadgeCreator(
            initial = b,
            behaviors = data.behaviors,
            isNew = data.badges.none { it.id == b.id },
            onDismiss = { creating = null },
            onSave = { vm.saveBadge(it); creating = null },
            onDelete = { vm.deleteBadge(b.id); creating = null },
        )
    }
}

@Composable
private fun BadgeCard(b: Badge, earned: EarnedBadge?, progress: BadgeProgress, description: String, onClick: () -> Unit) {
    ParchmentCard(Modifier.fillMaxWidth(), onClick = onClick, padding = 12.dp) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Medallion(b.icon, earned = earned != null)
            Spacer(Modifier.height(6.dp))
            Text(b.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center, color = Forest.Deep)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                color = Forest.BarkDark,
                modifier = Modifier.heightIn(min = 48.dp),
            )
            Spacer(Modifier.height(6.dp))
            if (earned != null) {
                Text("✅ ${formatDate(earned.time)}", style = MaterialTheme.typography.labelMedium, color = Forest.Moss, fontWeight = FontWeight.Bold)
            } else {
                ForestProgress(progress.fraction, height = 8.dp)
                Text(
                    "${progress.current} / ${progress.target} ${BadgeEngine.progressUnit(b.type, progress.target)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Forest.BarkLight,
                )
            }
        }
    }
}

@Composable
private fun BadgeCreator(
    initial: Badge,
    behaviors: List<Behavior>,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Badge) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var icon by remember { mutableStateOf(initial.icon) }
    var type by remember { mutableStateOf(initial.type) }
    val selected = remember { mutableStateMapOf<String, Int>().apply { initial.items.forEach { put(it.behaviorId, it.count) } } }
    var target by remember { mutableIntStateOf(initial.target.coerceAtLeast(1)) }
    var bonus by remember { mutableIntStateOf(initial.bonus) }
    var note by remember { mutableStateOf(initial.note) }
    var confirmDelete by remember { mutableStateOf(false) }

    val showCount = type == BadgeType.COMBO_DAY || type == BadgeType.STREAK
    val draft = initial.copy(
        name = name.trim(),
        icon = icon,
        type = type,
        items = behaviors.filter { it.id in selected }.map { BadgeItem(it.id, if (showCount) selected.getValue(it.id) else 1) },
        target = if (type == BadgeType.COMBO_DAY) 1 else target,
        bonus = bonus,
        note = note.trim(),
    )
    val ordered = if (type == BadgeType.ABSTAIN) behaviors.sortedBy { it.positive } else behaviors.sortedByDescending { it.positive }

    FullScreenDialog(
        title = if (isNew) "✨ Kreator odznak" else "Edytuj odznakę",
        onDismiss = onDismiss,
        saveEnabled = name.isNotBlank() && selected.isNotEmpty(),
        onSave = { onSave(draft) },
    ) {
        // podgląd
        ParchmentCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Medallion(icon, earned = true, size = 76.dp)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(name.ifBlank { "Nazwa odznaki" }, style = MaterialTheme.typography.titleLarge, color = Forest.Deep)
                    Text(BadgeEngine.describe(draft, behaviors), style = MaterialTheme.typography.bodyMedium)
                    if (bonus > 0) Text("+$bonus ${pointsWord(bonus)} bonusu 🌰", color = Forest.Bark, fontWeight = FontWeight.Bold)
                }
            }
        }

        SectionTitle("1. Nazwa i ikonka")
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text("Nazwa odznaki, np. „Poranny Bohater”") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        IconPicker(
            selected = icon,
            groups = listOf("Odznaki" to IconSets.badges, "Zachowania" to IconSets.positive),
            onSelect = { icon = it },
        )

        SectionTitle("2. Rodzaj odznaki")
        BadgeType.entries.forEach { t ->
            val sel = t == type
            Surface(
                onClick = { type = t },
                shape = RoundedCornerShape(18.dp),
                color = if (sel) Forest.LeafTint else Color.White.copy(alpha = 0.7f),
                border = BorderStroke(if (sel) 3.dp else 1.dp, if (sel) Forest.Moss else Forest.Bark.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(t.emoji, fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.title, fontWeight = FontWeight.Bold, color = Forest.Deep)
                        Text(t.explanation, style = MaterialTheme.typography.bodySmall)
                    }
                    RadioButton(selected = sel, onClick = { type = t })
                }
            }
        }

        SectionTitle("3. Zachowania")
        Text(
            when (type) {
                BadgeType.COMBO_DAY -> "Zaznacz zachowania, które trzeba wykonać jednego dnia. Możesz ustawić, ile razy każde."
                BadgeType.STREAK -> "Zaznacz zachowania, które trzeba wykonywać codziennie (np. mycie zębów ×2 dziennie)."
                BadgeType.TOTAL -> "Zaznacz zachowanie (lub kilka) – liczy się łączna liczba wykonań."
                BadgeType.ABSTAIN -> "Zaznacz złe zachowania, których ma nie być. Dni liczą się od utworzenia odznaki."
            },
            color = Forest.BarkDark,
        )
        ordered.forEach { b ->
            val isSel = b.id in selected
            Surface(
                onClick = { if (isSel) selected.remove(b.id) else selected[b.id] = 1 },
                shape = RoundedCornerShape(16.dp),
                color = when {
                    !isSel -> Color.White.copy(alpha = 0.6f)
                    b.positive -> Forest.LeafTint
                    else -> Forest.MushroomTint
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(Modifier.padding(horizontal = 6.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isSel, onCheckedChange = { if (it) selected[b.id] = 1 else selected.remove(b.id) })
                    Text(b.icon, fontSize = 24.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(b.name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (isSel && showCount) {
                        val c = selected[b.id] ?: 1
                        RoundButton("−", enabled = c > 1, size = 32.dp) { selected[b.id] = c - 1 }
                        Text("×$c", Modifier.widthIn(min = 36.dp), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        RoundButton("+", enabled = c < 10, size = 32.dp) { selected[b.id] = c + 1 }
                    }
                }
            }
        }

        if (type != BadgeType.COMBO_DAY) {
            SectionTitle("4. Warunek")
            Stepper(
                label = when (type) {
                    BadgeType.STREAK -> "Ile dni z rzędu"
                    BadgeType.TOTAL -> "Ile razy łącznie"
                    else -> "Ile dni bez wpadki"
                },
                value = target,
                onValueChange = { target = it },
                range = 1..365,
            )
        }

        SectionTitle(if (type == BadgeType.COMBO_DAY) "4. Nagroda za odznakę" else "5. Nagroda za odznakę")
        Stepper("Bonusowe punkty", bonus, { bonus = it }, 0..500, step = 5)
        OutlinedTextField(
            value = note,
            onValueChange = { note = it.take(120) },
            label = { Text("Własny opis (opcjonalnie)") },
            modifier = Modifier.fillMaxWidth(),
        )
        if (!isNew) {
            OutlinedButton(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest.Mushroom),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("🗑️ Usuń odznakę") }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Usunąć „${initial.name}”?",
            text = "Odznaka zniknie także z listy zdobytych.",
            confirmLabel = "Usuń",
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
