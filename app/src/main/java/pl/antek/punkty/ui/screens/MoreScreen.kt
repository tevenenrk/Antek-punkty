package pl.antek.punkty.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.PointEvent
import pl.antek.punkty.logic.Levels
import pl.antek.punkty.logic.signed
import pl.antek.punkty.ui.AppViewModel
import pl.antek.punkty.ui.components.*
import pl.antek.punkty.ui.theme.Forest

private enum class Danger { PROGRESS, ALL }

@Composable
fun MoreScreen(data: AppData, vm: AppViewModel, isParent: Boolean, requireParent: (() -> Unit) -> Unit) {
    var nameDialog by remember { mutableStateOf(false) }
    var pinDialog by remember { mutableStateOf(false) }
    var manualDialog by remember { mutableStateOf(false) }
    var danger by remember { mutableStateOf<Danger?>(null) }
    var deleteEvent by remember { mutableStateOf<PointEvent?>(null) }
    var showAll by remember { mutableStateOf(false) }
    val history = remember(data.events) { data.events.sortedByDescending { it.time } }
    val visible = if (showAll) history else history.take(30)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ParchmentCard(Modifier.fillMaxWidth()) {
                SectionTitle("⚙️ Ustawienia rodzica")
                Spacer(Modifier.height(8.dp))
                SettingRow("🧒", "Imię", data.childName, "Zmień") { requireParent { nameDialog = true } }
                SettingRow(
                    "🔐", "PIN rodzica",
                    if (data.pin.isNullOrEmpty()) "Brak – każdy może przyznawać punkty" else "Ustawiony",
                    if (data.pin.isNullOrEmpty()) "Ustaw" else "Zmień",
                ) { requireParent { pinDialog = true } }
                if (!data.pin.isNullOrEmpty()) {
                    TextButton(onClick = { requireParent { vm.setPin(null) } }) { Text("Usuń PIN") }
                }
                SettingRow("🎁", "Punkty ręcznie", "Bonus albo korekta", "Dodaj") { requireParent { manualDialog = true } }
            }
        }
        item {
            ParchmentCard(Modifier.fillMaxWidth()) {
                SectionTitle("🌳 Poziomy w lesie")
                Spacer(Modifier.height(8.dp))
                val current = Levels.forXp(data.xp)
                Levels.all.forEach { l ->
                    val reached = data.xp >= l.minXp
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp).alpha(if (reached) 1f else 0.5f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(l.icon, fontSize = 24.sp)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "${l.number}. ${l.name}",
                            Modifier.weight(1f),
                            fontWeight = if (l == current) FontWeight.ExtraBold else FontWeight.Normal,
                            color = if (l == current) Forest.Moss else Forest.Ink,
                        )
                        Text("⭐ ${l.minXp}", color = Forest.Bark)
                        if (l == current) Text("  ◀", color = Forest.Moss, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "Gwiazdki ⭐ to wszystkie zdobyte punkty – nie znikają po odebraniu nagrody.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Forest.BarkLight,
                )
            }
        }
        item {
            ParchmentCard(Modifier.fillMaxWidth()) {
                SectionTitle("📜 Historia")
                if (history.isEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text("Tu pojawią się wszystkie zdobyte i stracone punkty.", color = Forest.BarkDark)
                } else if (isParent) {
                    Text("Możesz usunąć pomyłkę ikoną kosza.", style = MaterialTheme.typography.bodySmall, color = Forest.BarkLight)
                }
            }
        }
        items(visible, key = { it.id }) { ev ->
            Surface(color = Forest.Parchment.copy(alpha = 0.92f), shape = MaterialTheme.shapes.medium) {
                Box(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                    EventRow(ev, showDate = true, onDelete = if (isParent) ({ deleteEvent = ev }) else null)
                }
            }
        }
        if (!showAll && history.size > visible.size) {
            item {
                OutlinedButton(onClick = { showAll = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Pokaż całą historię (${history.size})")
                }
            }
        }
        item {
            ParchmentCard(Modifier.fillMaxWidth()) {
                SectionTitle("🍂 Zacznij od nowa")
                Spacer(Modifier.height(6.dp))
                OutlinedButton(
                    onClick = { requireParent { danger = Danger.PROGRESS } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest.Mushroom),
                ) { Text("Wyzeruj punkty i odznaki") }
                OutlinedButton(
                    onClick = { requireParent { danger = Danger.ALL } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest.Mushroom),
                ) { Text("Przywróć ustawienia początkowe") }
            }
        }
    }

    if (nameDialog) {
        TextInputDialog(
            title = "Imię dziecka",
            initial = data.childName,
            label = "Imię",
            validate = { if (it.isBlank()) "Wpisz imię" else null },
            onDismiss = { nameDialog = false },
            onConfirm = { vm.setChildName(it); nameDialog = false },
        )
    }
    if (pinDialog) {
        TextInputDialog(
            title = "PIN rodzica",
            initial = "",
            label = "Nowy PIN (4–8 cyfr)",
            numeric = true,
            validate = { if (it.length < 4) "PIN musi mieć co najmniej 4 cyfry" else null },
            onDismiss = { pinDialog = false },
            onConfirm = { vm.setPin(it); pinDialog = false },
        )
    }
    if (manualDialog) {
        ManualPointsDialog(
            onDismiss = { manualDialog = false },
            onConfirm = { pts, note -> vm.manualAdjust(pts, note); manualDialog = false },
        )
    }
    deleteEvent?.let { ev ->
        ConfirmDialog(
            title = "Usunąć wpis?",
            text = "${ev.icon} ${ev.title} (${signed(ev.points)})",
            confirmLabel = "Usuń",
            onConfirm = { vm.removeEvent(ev.id); deleteEvent = null },
            onDismiss = { deleteEvent = null },
        )
    }
    danger?.let { d ->
        ConfirmDialog(
            title = if (d == Danger.PROGRESS) "Wyzerować postępy?" else "Przywrócić ustawienia początkowe?",
            text = if (d == Danger.PROGRESS) "Znikną wszystkie punkty, historia i zdobyte odznaki. Zachowania, nagrody i odznaki do zdobycia zostaną."
            else "Znikną punkty, historia oraz Twoje własne zachowania, nagrody i odznaki. Tego nie da się cofnąć.",
            confirmLabel = "Tak, wyczyść",
            icon = "🍂",
            onConfirm = {
                if (d == Danger.PROGRESS) vm.resetProgress() else vm.resetAll()
                danger = null
            },
            onDismiss = { danger = null },
        )
    }
}

@Composable
private fun SettingRow(icon: String, title: String, value: String, action: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 24.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(value, style = MaterialTheme.typography.bodySmall, color = Forest.BarkLight)
        }
        FilledTonalButton(onClick = onClick) { Text(action) }
    }
}

@Composable
private fun ManualPointsDialog(onDismiss: () -> Unit, onConfirm: (Int, String) -> Unit) {
    var points by remember { mutableIntStateOf(5) }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest.Parchment,
        icon = { Text(if (points >= 0) "🎁" else "✂️", fontSize = 36.sp) },
        title = { Text("Punkty ręcznie") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Stepper("Punkty", points, { points = if (it == 0) (if (points > 0) -1 else 1) else it }, -200..200)
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(40) },
                    label = { Text("Za co? (opcjonalnie)") },
                    singleLine = true,
                )
            }
        },
        confirmButton = { Button(onClick = { onConfirm(points, note) }) { Text(signed(points)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}
