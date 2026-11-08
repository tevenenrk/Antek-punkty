package pl.antek.punkty.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import pl.antek.punkty.data.AppData
import pl.antek.punkty.data.PointEvent
import pl.antek.punkty.logic.pointsWord
import pl.antek.punkty.logic.signed
import pl.antek.punkty.ui.components.CelebrationDialog
import pl.antek.punkty.ui.components.ForestBackground
import pl.antek.punkty.ui.components.PinDialog
import pl.antek.punkty.ui.components.PointsPill
import pl.antek.punkty.ui.screens.BadgesScreen
import pl.antek.punkty.ui.screens.BehaviorsScreen
import pl.antek.punkty.ui.screens.HomeScreen
import pl.antek.punkty.ui.screens.MoreScreen
import pl.antek.punkty.ui.screens.RewardsScreen
import pl.antek.punkty.ui.theme.Forest

enum class AppTab(val label: String, val emoji: String) {
    HOME("Las", "🌳"),
    BEHAVIORS("Zachowania", "⭐"),
    REWARDS("Nagrody", "🎁"),
    BADGES("Odznaki", "🏅"),
    MORE("Więcej", "📜"),
}

@Composable
fun ForestApp(vm: AppViewModel = viewModel()) {
    val data by vm.data.collectAsStateWithLifecycle()
    val isParent by vm.isParent.collectAsStateWithLifecycle()
    var tab by rememberSaveable { mutableStateOf(AppTab.HOME) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    val celebrations = remember { mutableStateListOf<Celebration>() }

    LaunchedEffect(Unit) { vm.celebrations.collect { celebrations.add(it) } }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { vm.refresh() }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { vm.lock() }

    // Akcje rodzica: jeśli ustawiono PIN, najpierw pytamy o niego.
    val requireParent: (() -> Unit) -> Unit = { action ->
        if (isParent) action() else pendingAction = action
    }
    val onLogged: (PointEvent) -> Unit = { ev ->
        scope.launch {
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "${ev.icon} ${ev.title}: ${signed(ev.points)} ${pointsWord(ev.points)}",
                actionLabel = "Cofnij",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) vm.removeEvent(ev.id)
        }
    }

    ForestBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopBar(data, isParent) {
                    if (isParent) vm.lock() else pendingAction = {}
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Forest.Deep) {
                    AppTab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t,
                            onClick = { tab = t },
                            icon = { Text(t.emoji, fontSize = 22.sp) },
                            label = { Text(t.label, maxLines = 1, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedTextColor = Forest.Gold,
                                unselectedTextColor = Color.White.copy(alpha = 0.75f),
                                indicatorColor = Forest.Moss,
                            ),
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(snackbar) { d ->
                    Snackbar(
                        snackbarData = d,
                        containerColor = Forest.BarkDark,
                        contentColor = Forest.Parchment,
                        actionColor = Forest.Gold,
                        shape = CircleShape,
                    )
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (tab) {
                    AppTab.HOME -> HomeScreen(data, onGoToBehaviors = { tab = AppTab.BEHAVIORS })
                    AppTab.BEHAVIORS -> BehaviorsScreen(data, vm, requireParent, onLogged)
                    AppTab.REWARDS -> RewardsScreen(data, vm, requireParent)
                    AppTab.BADGES -> BadgesScreen(data, vm, requireParent)
                    AppTab.MORE -> MoreScreen(data, vm, isParent, requireParent)
                }
            }
        }
    }

    pendingAction?.let { action ->
        PinDialog(
            onDismiss = { pendingAction = null },
            onSubmit = { pin ->
                if (vm.unlock(pin)) {
                    pendingAction = null
                    action()
                    true
                } else false
            },
        )
    }

    celebrations.firstOrNull()?.let { c ->
        key(c) {
            CelebrationDialog(c, data.behaviors) { celebrations.removeAt(0) }
        }
    }
}

@Composable
private fun TopBar(data: AppData, isParent: Boolean, onLockClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("🌲", fontSize = 28.sp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text("Leśne Punkty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Forest.Deep)
            Text("Przygoda: ${data.childName}", style = MaterialTheme.typography.labelMedium, color = Forest.Bark)
        }
        PointsPill(data.balance)
        if (!data.pin.isNullOrEmpty()) {
            Spacer(Modifier.width(8.dp))
            Surface(
                onClick = onLockClick,
                shape = CircleShape,
                color = if (isParent) Forest.Gold else Forest.Parchment,
                border = BorderStroke(2.dp, Forest.Bark.copy(alpha = 0.4f)),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(if (isParent) "🔓" else "🔒", fontSize = 20.sp)
                }
            }
        }
    }
}
