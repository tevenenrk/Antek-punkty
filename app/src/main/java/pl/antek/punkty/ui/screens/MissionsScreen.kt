package pl.antek.punkty.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.IconSets
import pl.antek.punkty.data.Mission
import pl.antek.punkty.data.MissionStep
import pl.antek.punkty.data.newId
import pl.antek.punkty.logic.pointsWord
import pl.antek.punkty.ui.AppViewModel
import pl.antek.punkty.ui.components.*
import pl.antek.punkty.ui.theme.Forest

@Composable
fun MissionsScreen(data: AppData, vm: AppViewModel, requireParent: (() -> Unit) -> Unit) {
    var editing by remember { mutableStateOf<Mission?>(null) }
    var deleting by remember { mutableStateOf<Mission?>(null) }
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var showDone by rememberSaveable { mutableStateOf(false) }

    val active = data.missions.filter { it.completedAt == null }.sortedBy { it.createdAt }
    val done = data.missions.filter { it.completedAt != null }.sortedByDescending { it.completedAt }
    val onBoard = active.find { it.id == selectedId } ?: active.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            MissionBoardScene(
                title = onBoard?.title,
                done = onBoard?.doneCount ?: 0,
                total = onBoard?.steps?.size ?: 0,
                reward = onBoard?.reward,
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    SectionTitle("🎯 Misje specjalne")
                    Text(
                        if (active.isEmpty()) "Brak aktywnych misji" else "Aktywne: ${active.size}",
                        color = Forest.BarkDark,
                    )
                }
                Button(
                    onClick = { requireParent { editing = Mission(newId(), "", steps = listOf(MissionStep(newId(), ""))) } },
                    colors = ButtonDefaults.buttonColors(containerColor = Forest.Moss),
                ) { Text("✨ Nowa misja") }
            }
        }
        if (active.isEmpty()) {
            item {
                ParchmentCard(Modifier.fillMaxWidth()) {
                    Text(
                        "Miś Kapral czeka na rozkazy! Stwórz misję z kilkoma podpunktami – za wykonanie wszystkich będą punkty.",
                        color = Forest.BarkDark,
                    )
                }
            }
        }
        items(active, key = { it.id }) { m ->
            MissionCard(
                mission = m,
                onBoard = m.id == onBoard?.id,
                onSelect = { selectedId = m.id },
                onToggle = { stepId -> requireParent { vm.toggleStep(m.id, stepId) } },
                onEdit = { requireParent { editing = m } },
            )
        }
        if (done.isNotEmpty()) {
            item {
                TextButton(onClick = { showDone = !showDone }) {
                    Text(
                        (if (showDone) "▾ " else "▸ ") + "Wykonane misje (${done.size})",
                        fontWeight = FontWeight.Bold,
                        color = Forest.Deep,
                    )
                }
            }
            if (showDone) {
                items(done, key = { it.id }) { m ->
                    Surface(color = Forest.Parchment.copy(alpha = 0.92f), shape = MaterialTheme.shapes.medium) {
                        Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Medallion(m.icon, earned = true, size = 44.dp)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(m.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    "✅ ${formatDate(m.completedAt ?: 0)}" + if (m.reward > 0) " · +${m.reward} 🌰" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Forest.Moss,
                                )
                            }
                            IconButton(onClick = { requireParent { deleting = m } }) {
                                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Forest.BarkLight)
                            }
                        }
                    }
                }
            }
        }
    }

    editing?.let { m ->
        MissionEditor(
            initial = m,
            isNew = data.missions.none { it.id == m.id },
            onDismiss = { editing = null },
            onSave = { vm.saveMission(it); selectedId = it.id; editing = null },
            onDelete = { deleting = m; editing = null },
        )
    }
    deleting?.let { m ->
        ConfirmDialog(
            title = "Usunąć misję „${m.title}”?",
            text = "Zdobyte za nią punkty zostaną w historii.",
            confirmLabel = "Usuń",
            onConfirm = { vm.deleteMission(m.id); deleting = null },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun MissionCard(
    mission: Mission,
    onBoard: Boolean,
    onSelect: () -> Unit,
    onToggle: (String) -> Unit,
    onEdit: () -> Unit,
) {
    val total = mission.steps.size
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(22.dp),
        color = Forest.Parchment.copy(alpha = 0.96f),
        border = BorderStroke(if (onBoard) 3.dp else 2.dp, if (onBoard) Forest.Gold else Forest.Bark.copy(alpha = 0.22f)),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(mission.icon, fontSize = 36.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(mission.title, style = MaterialTheme.typography.titleMedium, color = Forest.Deep)
                    if (mission.description.isNotBlank()) {
                        Text(mission.description, style = MaterialTheme.typography.bodySmall, color = Forest.BarkDark)
                    }
                }
                if (mission.reward > 0) {
                    Surface(shape = CircleShape, color = Forest.Gold.copy(alpha = 0.3f)) {
                        Text(
                            "🌰 ${mission.reward}",
                            Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontWeight = FontWeight.ExtraBold,
                            color = Forest.BarkDark,
                        )
                    }
                }
                IconButton(onClick = onEdit) { Text("✏️", fontSize = 18.sp) }
            }
            Spacer(Modifier.height(8.dp))
            ForestProgress(if (total == 0) 0f else mission.doneCount.toFloat() / total, height = 10.dp)
            Text(
                "Wykonane ${mission.doneCount} z $total",
                style = MaterialTheme.typography.labelMedium,
                color = Forest.BarkLight,
            )
            Spacer(Modifier.height(4.dp))
            mission.steps.forEach { step ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onToggle(step.id) }
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = step.done,
                        onCheckedChange = { onToggle(step.id) },
                        colors = CheckboxDefaults.colors(checkedColor = Forest.Moss),
                    )
                    Text(
                        step.text,
                        color = if (step.done) Forest.BarkLight else Forest.Ink,
                        textDecoration = if (step.done) TextDecoration.LineThrough else null,
                        fontWeight = if (step.done) FontWeight.Normal else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun MissionEditor(
    initial: Mission,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Mission) -> Unit,
    onDelete: () -> Unit,
) {
    var title by remember { mutableStateOf(initial.title) }
    var icon by remember { mutableStateOf(initial.icon) }
    var description by remember { mutableStateOf(initial.description) }
    var reward by remember { mutableIntStateOf(initial.reward) }
    val steps = remember { mutableStateListOf<MissionStep>().apply { addAll(initial.steps) } }
    val validSteps = steps.filter { it.text.isNotBlank() }

    FullScreenDialog(
        title = if (isNew) "🎯 Nowa misja" else "Edytuj misję",
        onDismiss = onDismiss,
        saveEnabled = title.isNotBlank() && validSteps.isNotEmpty(),
        onSave = {
            onSave(
                initial.copy(
                    title = title.trim(),
                    icon = icon,
                    description = description.trim(),
                    reward = reward,
                    steps = validSteps.map { it.copy(text = it.text.trim()) },
                ),
            )
        },
    ) {
        MissionBoardScene(
            title = title.ifBlank { null },
            done = validSteps.count { it.done },
            total = validSteps.size,
            reward = reward,
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it.take(50) },
            label = { Text("Nazwa misji, np. „Operacja Czysty Pokój”") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = description,
            onValueChange = { description = it.take(120) },
            label = { Text("Opis (opcjonalnie)") },
            modifier = Modifier.fillMaxWidth(),
        )

        SectionTitle("📋 Podpunkty")
        steps.forEachIndexed { i, step ->
            key(step.id) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${i + 1}.", fontWeight = FontWeight.Bold, color = Forest.Bark, modifier = Modifier.width(28.dp))
                    OutlinedTextField(
                        value = step.text,
                        onValueChange = { steps[i] = step.copy(text = it.take(80)) },
                        placeholder = { Text("Co trzeba zrobić?") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { steps.removeAt(i) }, enabled = steps.size > 1) {
                        Icon(Icons.Default.Close, contentDescription = "Usuń podpunkt", tint = Forest.BarkLight)
                    }
                }
            }
        }
        AddButton("Dodaj podpunkt", Modifier.fillMaxWidth()) { steps.add(MissionStep(newId(), "")) }

        SectionTitle("🌰 Nagroda za całą misję")
        Stepper("Punkty", reward, { reward = it }, 0..500, step = 5)
        Text("Za ${reward} ${pointsWord(reward)} po odhaczeniu wszystkich podpunktów.", color = Forest.BarkLight)

        SectionTitle("Ikonka")
        IconPicker(
            selected = icon,
            groups = listOf("Misje" to IconSets.missions, "Inne" to IconSets.positive),
            onSelect = { icon = it },
        )
        if (!isNew) {
            OutlinedButton(
                onClick = onDelete,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest.Mushroom),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("🗑️ Usuń misję") }
        }
        Spacer(Modifier.height(24.dp))
    }
}
