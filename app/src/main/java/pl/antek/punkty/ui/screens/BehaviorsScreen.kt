package pl.antek.punkty.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.Behavior
import pl.antek.punkty.data.IconSets
import pl.antek.punkty.data.PointEvent
import pl.antek.punkty.data.newId
import pl.antek.punkty.logic.BadgeEngine
import pl.antek.punkty.ui.AppViewModel
import pl.antek.punkty.ui.components.*
import pl.antek.punkty.ui.theme.Forest

@Composable
fun BehaviorsScreen(
    data: AppData,
    vm: AppViewModel,
    requireParent: (() -> Unit) -> Unit,
    onLogged: (PointEvent) -> Unit,
) {
    var positive by rememberSaveable { mutableStateOf(true) }
    var editMode by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Behavior?>(null) }
    val list = data.behaviors.filter { it.positive == positive }
    val streaks = remember(data.events) { BadgeEngine.behaviorStreaks(data) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ToggleChip("😊 Dobre", positive, Forest.Moss) { positive = true }
            Spacer(Modifier.width(8.dp))
            ToggleChip("😠 Złe", !positive, Forest.Mushroom) { positive = false }
            Spacer(Modifier.weight(1f))
            EditToggle(editMode) { if (editMode) editMode = false else requireParent { editMode = true } }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(108.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White.copy(alpha = 0.75f)) {
                    Text(
                        when {
                            editMode -> "✏️ Dotknij kafelka, żeby go zmienić, albo dodaj nowy."
                            positive -> "🌟 Dotknij, gdy uda się coś dobrego – punkty wpadną do skarbonki!"
                            else -> "🌧️ Dotknij, gdy zdarzy się coś złego – punkty uciekną ze skarbonki."
                        },
                        Modifier.padding(12.dp),
                        color = Forest.Deep,
                    )
                }
            }
            items(list, key = { it.id }) { b ->
                BehaviorTile(b, streaks[b.id] ?: 0, editMode) {
                    if (editMode) editing = b
                    else requireParent { onLogged(vm.logBehavior(b)) }
                }
            }
            if (editMode) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    AddButton(if (positive) "Nowe dobre zachowanie" else "Nowe złe zachowanie", Modifier.fillMaxWidth()) {
                        editing = Behavior(newId(), "", if (positive) "⭐" else "😡", if (positive) 2 else 3, positive)
                    }
                }
            }
            if (list.isEmpty() && !editMode) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text("Brak zachowań. Włącz edycję, żeby dodać pierwsze.", color = Forest.BarkDark, textAlign = TextAlign.Center)
                }
            }
        }
    }

    editing?.let { b ->
        BehaviorEditor(
            initial = b,
            isNew = data.behaviors.none { it.id == b.id },
            onDismiss = { editing = null },
            onSave = { vm.saveBehavior(it); editing = null },
            onDelete = { vm.deleteBehavior(b.id); editing = null },
        )
    }
}

@Composable
private fun BehaviorTile(b: Behavior, streak: Int, editMode: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.9f else 1f, label = "scale")
    val bg = if (b.positive) Forest.LeafTint else Forest.MushroomTint
    val accent = if (b.positive) Forest.Moss else Forest.Mushroom
    Surface(
        onClick = onClick,
        interactionSource = interaction,
        modifier = Modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .aspectRatio(0.82f),
        shape = RoundedCornerShape(22.dp),
        color = bg,
        border = BorderStroke(2.dp, accent.copy(alpha = 0.5f)),
        shadowElevation = 3.dp,
    ) {
        Box {
            Column(
                Modifier.fillMaxSize().padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(b.icon, fontSize = 40.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    b.name,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                    color = Forest.Deep,
                )
                Spacer(Modifier.height(6.dp))
                Surface(color = accent, shape = CircleShape) {
                    Text(
                        if (b.positive) "+${b.points}" else "−${b.points}",
                        Modifier.padding(horizontal = 12.dp, vertical = 2.dp),
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                    )
                }
            }
            if (streak >= 2 && b.positive) {
                Text("🔥$streak", Modifier.align(Alignment.TopStart).padding(8.dp), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            if (editMode) {
                Text("✏️", Modifier.align(Alignment.TopEnd).padding(8.dp))
            }
        }
    }
}

@Composable
private fun BehaviorEditor(
    initial: Behavior,
    isNew: Boolean,
    onDismiss: () -> Unit,
    onSave: (Behavior) -> Unit,
    onDelete: () -> Unit,
) {
    var name by remember { mutableStateOf(initial.name) }
    var icon by remember { mutableStateOf(initial.icon) }
    var points by remember { mutableIntStateOf(initial.points) }
    var positive by remember { mutableStateOf(initial.positive) }
    var confirmDelete by remember { mutableStateOf(false) }

    FullScreenDialog(
        title = if (isNew) "Nowe zachowanie" else "Edytuj zachowanie",
        onDismiss = onDismiss,
        saveEnabled = name.isNotBlank(),
        onSave = { onSave(initial.copy(name = name.trim(), icon = icon, points = points, positive = positive)) },
    ) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Box(Modifier.width(130.dp)) {
                BehaviorTile(Behavior("", name.ifBlank { "Nazwa" }, icon, points, positive), 0, false) {}
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { name = it.take(40) },
            label = { Text("Nazwa zachowania") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row {
            ToggleChip("😊 Dobre", positive, Forest.Moss) { positive = true }
            Spacer(Modifier.width(8.dp))
            ToggleChip("😠 Złe", !positive, Forest.Mushroom) { positive = false }
        }
        Stepper(
            label = if (positive) "Ile punktów dodać" else "Ile punktów odjąć",
            value = points,
            onValueChange = { points = it },
            range = 1..100,
        )
        SectionTitle("Ikonka")
        IconPicker(
            selected = icon,
            groups = if (positive) listOf("Dobre zachowania" to IconSets.positive, "Złe zachowania" to IconSets.negative)
            else listOf("Złe zachowania" to IconSets.negative, "Dobre zachowania" to IconSets.positive),
            onSelect = { icon = it },
        )
        if (!isNew) {
            OutlinedButton(
                onClick = { confirmDelete = true },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Forest.Mushroom),
                modifier = Modifier.fillMaxWidth(),
            ) { Text("🗑️ Usuń zachowanie") }
        }
        Spacer(Modifier.height(24.dp))
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = "Usunąć „${initial.name}”?",
            text = "Historia punktów zostanie zachowana.",
            confirmLabel = "Usuń",
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}
