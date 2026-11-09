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
import pl.antek.punkty.ui.SyncUi
import pl.antek.punkty.sync.FamilyCode
import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import pl.antek.punkty.ui.components.*
import pl.antek.punkty.ui.theme.Forest

private enum class Danger { PROGRESS, ALL }

@Composable
fun MoreScreen(data: AppData, vm: AppViewModel, isParent: Boolean, syncUi: SyncUi, requireParent: (() -> Unit) -> Unit) {
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
        item { SyncCard(syncUi, vm, requireParent) }
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

@Composable
private fun SyncCard(sync: SyncUi, vm: AppViewModel, requireParent: (() -> Unit) -> Unit) {
    var joinDialog by remember { mutableStateOf(false) }
    var confirmCreate by remember { mutableStateOf(false) }
    var confirmLeave by remember { mutableStateOf(false) }
    val context = LocalContext.current

    ParchmentCard(Modifier.fillMaxWidth()) {
        SectionTitle("📱 Wspólne punkty")
        Spacer(Modifier.height(4.dp))
        when {
            !sync.available -> Text(
                "Ta wersja aplikacji nie ma jeszcze połączenia z chmurą (brak konfiguracji Firebase). " +
                    "Po jej dodaniu dwa telefony będą mogły dzielić punkty za pomocą kodu rodziny.",
                color = Forest.BarkDark,
            )

            sync.code == null -> {
                Text(
                    "Połącz telefony rodziców kodem rodziny – wszystkie będą miały te same punkty, nagrody i odznaki. " +
                        "Bez zakładania kont.",
                    color = Forest.BarkDark,
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { requireParent { confirmCreate = true } },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Forest.Moss),
                ) { Text("🌱 Utwórz kod rodziny (pierwszy telefon)") }
                OutlinedButton(
                    onClick = { requireParent { joinDialog = true } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !sync.busy,
                ) { Text(if (sync.busy) "Łączenie…" else "🔑 Mam kod – dołącz (drugi telefon)") }
            }

            else -> {
                Text("Kod rodziny – wpisz go na drugim telefonie:", color = Forest.BarkDark)
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    border = BorderStroke(2.dp, Forest.Gold),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        FamilyCode.pretty(sync.code),
                        Modifier.padding(vertical = 14.dp),
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 28.sp,
                        letterSpacing = 2.sp,
                        color = Forest.Deep,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (sync.online) Forest.Leaf else Color(0xFFBDBDBD)),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (sync.online) "Połączono – zmiany od razu trafiają na inne telefony"
                        else "Brak internetu – zmiany wyślą się, gdy wróci połączenie",
                        style = MaterialTheme.typography.bodySmall,
                        color = Forest.BarkDark,
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(onClick = {
                        val text = "Kod rodziny do aplikacji Leśne Punkty: ${FamilyCode.pretty(sync.code)}"
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                        context.startActivity(Intent.createChooser(send, "Wyślij kod rodziny"))
                    }) { Text("📤 Wyślij kod") }
                    TextButton(onClick = { requireParent { confirmLeave = true } }) { Text("Odłącz ten telefon") }
                }
            }
        }
        sync.error?.let { err ->
            Spacer(Modifier.height(8.dp))
            Surface(shape = RoundedCornerShape(12.dp), color = Forest.MushroomTint, onClick = { vm.clearSyncError() }) {
                Text("⚠️ $err", Modifier.padding(10.dp), color = Forest.Mushroom)
            }
        }
    }

    if (confirmCreate) {
        ConfirmDialog(
            title = "Utworzyć kod rodziny?",
            text = "Punkty, nagrody i odznaki z tego telefonu trafią do chmury. Na drugim telefonie wybierz „Mam kod – dołącz”.",
            confirmLabel = "Utwórz",
            icon = "🌱",
            onConfirm = { confirmCreate = false; vm.createFamily() },
            onDismiss = { confirmCreate = false },
        )
    }
    if (confirmLeave) {
        ConfirmDialog(
            title = "Odłączyć ten telefon?",
            text = "Telefon zachowa obecne dane, ale przestanie je wymieniać z innymi. Możesz później dołączyć ponownie tym samym kodem.",
            confirmLabel = "Odłącz",
            onConfirm = { confirmLeave = false; vm.leaveFamily() },
            onDismiss = { confirmLeave = false },
        )
    }
    if (joinDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { joinDialog = false },
            containerColor = Forest.Parchment,
            icon = { Text("🔑", fontSize = 36.sp) },
            title = { Text("Dołącz do rodziny") },
            text = {
                Column {
                    Text("Wpisz kod z pierwszego telefonu. Dane na TYM telefonie zostaną zastąpione danymi rodziny.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it.uppercase().take(14) },
                        label = { Text("Kod, np. ABCDE-FGH23") },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 20.sp),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { joinDialog = false; vm.joinFamily(code) },
                    enabled = FamilyCode.normalize(code) != null,
                ) { Text("Dołącz") }
            },
            dismissButton = { TextButton(onClick = { joinDialog = false }) { Text("Anuluj") } },
        )
    }
}
