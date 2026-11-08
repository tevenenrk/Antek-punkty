package pl.antek.punkty.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.IconSets
import pl.antek.punkty.data.Reward
import pl.antek.punkty.data.newId
import pl.antek.punkty.logic.pointsWord
import pl.antek.punkty.ui.AppViewModel
import pl.antek.punkty.ui.components.*
import pl.antek.punkty.ui.theme.Forest

@Composable
fun RewardsScreen(data: AppData, vm: AppViewModel, requireParent: (() -> Unit) -> Unit) {
    var editMode by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Reward?>(null) }
    var confirm by remember { mutableStateOf<Reward?>(null) }
    val rewards = data.rewards.sortedBy { it.cost }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            ParchmentCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        SectionTitle("🎁 Leśny Skarbiec")
                        Text("Wymień zebrane żołędzie na nagrody!", color = Forest.BarkDark)
                    }
                    EditToggle(editMode) { if (editMode) editMode = false else requireParent { editMode = true } }
                }
            }
        }
        items(rewards, key = { it.id }) { r ->
            RewardCard(r, data.balance, editMode, onRedeem = { confirm = r }, onEdit = { editing = r })
        }
        if (editMode) {
            item {
                AddButton("Nowa nagroda", Modifier.fillMaxWidth()) { editing = Reward(newId(), "", "🎁", 20) }
            }
        }
        if (rewards.isEmpty() && !editMode) {
            item { Text("Skarbiec jest pusty. Włącz edycję, żeby dodać nagrody.", color = Forest.BarkDark) }
        }
    }

    confirm?.let { r ->
        ConfirmDialog(
            title = "Odebrać nagrodę?",
            text = "${r.icon} ${r.name}\nza ${r.cost} ${pointsWord(r.cost)}",
            confirmLabel = "Odbierz",
            icon = r.icon,
            onConfirm = {
                confirm = null
                requireParent { vm.redeem(r) }
            },
            onDismiss = { confirm = null },
        )
    }
    editing?.let { r ->
        RewardEditor(
            initial = r,
            isNew = data.rewards.none { it.id == r.id },
            onDismiss = { editing = null },
            onSave = { vm.saveReward(it); editing = null },
            onDelete = { vm.deleteReward(r.id); editing = null },
        )
    }
}

@Composable
private fun RewardCard(r: Reward, balance: Int, editMode: Boolean, onRedeem: () -> Unit, onEdit: () -> Unit) {
    val affordable = balance >= r.cost
    ParchmentCard(Modifier.fillMaxWidth(), onClick = if (editMode) onEdit else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(if (affordable) Forest.Gold.copy(alpha = 0.35f) else Forest.LeafTint),
                contentAlignment = Alignment.Center,
            ) { Text(r.icon, fontSize = 36.sp) }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(r.name, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("🌰 ${r.cost} ${pointsWord(r.cost)}", color = Forest.Bark, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                ForestProgress(if (r.cost <= 0) 1f else balance.toFloat() / r.cost, height = 10.dp)
                if (!affordable) {
                    val missing = r.cost - balance.coerceAtLeast(0)
                    Text("Brakuje $missing ${pointsWord(missing)}", style = MaterialTheme.typography.bodySmall, color = Forest.BarkLight)
                }
            }
            Spacer(Modifier.width(8.dp))
            if (editMode) {
                Text("✏️", fontSize = 24.sp)
            } else {
                Button(
                    onClick = onRedeem,
                    enabled = affordable,
                    colors = ButtonDefaults.buttonColors(containerColor = Forest.Moss),
                    contentPadding = PaddingValues(horizontal = 14.dp),
                ) { Text("Odbierz") }
            }
        }
    }
}

@Composable
private fun RewardEditor(
    initial: Reward,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Reward) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var icon by remember { mutableStateOf(initial.icon) }
    var cost by remember { mutableIntStateOf(initial.cost) }
    var confirmDelete by remember { mutableStateOf(false) }

    FullScreenDialog(
        title = if (isNew) "Nowa nagroda" else "Edytuj nagrodę",
        onDismiss = onDismiss,
        saveEnabled = name.isNotBlank(),
        onSave = { onSave(initial.copy(name = name.trim(), icon = icon, cost = cost)) },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(icon, fontSize = 56.sp)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(name.ifBlank { "Nazwa nagrody" }, style = MaterialTheme.typography.titleLarge)
                Text("🌰 $cost ${pointsWord(cost)}", color = Forest.Bark, fontWeight = FontWeight.Bold)
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text("Nazwa nagrody") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Stepper("Cena (co 1)", cost, { cost = it }, 1..5000, step = 1)
        Stepper("Cena (co 10)", cost, { cost = it }, 1..5000, step = 10)
        SectionTitle("Ikonka")
        IconPicker(
            selected = icon,
            groups = listOf("Nagrody" to IconSets.rewards, "Inne" to IconSets.badges),
            onSelect = { icon = it },
        )
        if (!isNew) {
            OutlinedButton(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest.Mushroom),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("🗑️ Usuń nagrodę") }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Usunąć „${initial.name}”?",
            text = "Nagroda zniknie ze Skarbca.",
            confirmLabel = "Usuń",
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
