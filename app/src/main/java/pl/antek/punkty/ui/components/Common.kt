package pl.antek.punkty.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pl.antek.punkty.data.PointEvent
import pl.antek.punkty.logic.signed
import pl.antek.punkty.ui.theme.Forest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val polish = Locale.forLanguageTag("pl")
private val dateFmt = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm", polish)
private val timeFmt = DateTimeFormatter.ofPattern("HH:mm", polish)
private val shortDateFmt = DateTimeFormatter.ofPattern("d MMM yyyy", polish)

fun formatDateTime(ts: Long): String = dateFmt.format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()))
fun formatTime(ts: Long): String = timeFmt.format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()))
fun formatDate(ts: Long): String = shortDateFmt.format(Instant.ofEpochMilli(ts).atZone(ZoneId.systemDefault()))

@Composable
fun ParchmentCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val color = Forest.Parchment.copy(alpha = 0.96f)
    val border = BorderStroke(2.dp, Forest.Bark.copy(alpha = 0.22f))
    if (onClick != null) {
        Surface(onClick = onClick, modifier = modifier, shape = shape, color = color, border = border, shadowElevation = 3.dp) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color, border = border, shadowElevation = 3.dp) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

/** Drewniana tabliczka – nagłówek z profilem dziecka. */
@Composable
fun WoodenSign(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(26.dp)
    Surface(modifier = modifier, shape = shape, shadowElevation = 6.dp, color = Color.Transparent) {
        Column(
            Modifier
                .background(Brush.verticalGradient(listOf(Color(0xFF9C7A5B), Forest.Bark, Color(0xFF5D4037))), shape)
                .border(3.dp, Forest.BarkDark, shape)
                .padding(18.dp),
            content = content,
        )
    }
}

@Composable
fun PointsPill(points: Int, modifier: Modifier = Modifier, big: Boolean = false) {
    val animated by animateIntAsState(points, animationSpec = tween(700), label = "points")
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = Color(0xFFFFE9A8),
        border = BorderStroke(2.dp, Forest.Gold),
        shadowElevation = 2.dp,
    ) {
        Row(
            Modifier.padding(horizontal = if (big) 16.dp else 12.dp, vertical = if (big) 8.dp else 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("🌰", fontSize = if (big) 26.sp else 18.sp)
            Spacer(Modifier.width(6.dp))
            Text(
                "$animated",
                fontSize = if (big) 28.sp else 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (points < 0) Forest.Mushroom else Forest.BarkDark,
            )
        }
    }
}

@Composable
fun ForestProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = Forest.Leaf,
    trackColor: Color = Forest.Bark.copy(alpha = 0.15f),
    height: Dp = 12.dp,
) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), animationSpec = tween(800), label = "progress")
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clip(CircleShape)
            .background(trackColor),
    ) {
        if (f > 0f) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(f)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.8f), color))),
            )
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, modifier = modifier, style = MaterialTheme.typography.titleLarge, color = Forest.Deep)
}

@Composable
fun RoundButton(text: String, enabled: Boolean = true, size: Dp = 40.dp, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = if (enabled) Forest.Leaf.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.06f),
        modifier = Modifier.size(size),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = if (enabled) Forest.Deep else Color.Gray)
        }
    }
}

@Composable
fun Stepper(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    step: Int = 1,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        RoundButton("−", enabled = value > range.first) { onValueChange((value - step).coerceIn(range)) }
        Text(
            "$value",
            Modifier.widthIn(min = 56.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.ExtraBold,
        )
        RoundButton("+", enabled = value < range.last) { onValueChange((value + step).coerceIn(range)) }
    }
}

@Composable
fun ToggleChip(text: String, selected: Boolean, color: Color, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        color = if (selected) color else Forest.Parchment.copy(alpha = 0.9f),
        border = BorderStroke(2.dp, color),
        shadowElevation = if (selected) 3.dp else 0.dp,
    ) {
        Text(
            text,
            Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (selected) Color.White else color,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun EditToggle(active: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) Forest.Gold else Forest.Parchment.copy(alpha = 0.9f),
        border = BorderStroke(2.dp, Forest.Bark.copy(alpha = 0.4f)),
    ) {
        Text(
            if (active) "✅ Gotowe" else "✏️ Edytuj",
            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            fontWeight = FontWeight.Bold,
            color = Forest.BarkDark,
        )
    }
}

@Composable
fun AddButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Forest.LeafTint.copy(alpha = 0.9f),
        border = BorderStroke(2.dp, Forest.Moss.copy(alpha = 0.6f)),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text("➕", fontSize = 20.sp)
            Spacer(Modifier.width(8.dp))
            Text(text, fontWeight = FontWeight.Bold, color = Forest.Deep)
        }
    }
}

/** Okrągły medalion odznaki. Niezdobyta jest przygaszona z kłódką. */
@Composable
fun Medallion(icon: String, earned: Boolean, size: Dp = 72.dp) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(size)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(
                    if (earned) Brush.radialGradient(listOf(Color(0xFFFFF4C2), Color(0xFFF7C948), Color(0xFFE0A100)))
                    else Brush.radialGradient(listOf(Color(0xFFEDEDE4), Color(0xFFCFCFC2))),
                )
                .border(3.dp, if (earned) Color(0xFFB8860B) else Color(0xFFAAAA9A), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                icon,
                fontSize = (size.value * 0.5f).sp,
                modifier = Modifier.alpha(if (earned) 1f else 0.35f),
            )
        }
        if (!earned) {
            Text(
                "🔒",
                fontSize = (size.value * 0.22f).sp,
                modifier = Modifier.align(Alignment.BottomEnd),
            )
        }
    }
}

@Composable
fun EventRow(ev: PointEvent, showDate: Boolean, onDelete: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(ev.icon, fontSize = 26.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(ev.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                if (showDate) formatDateTime(ev.time) else formatTime(ev.time),
                style = MaterialTheme.typography.bodySmall,
                color = Forest.BarkLight,
            )
        }
        Text(
            signed(ev.points),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = if (ev.points >= 0) Forest.Moss else Forest.Mushroom,
        )
        if (onDelete != null) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Usuń", tint = Forest.BarkLight)
            }
        }
    }
}

@Composable
fun FullScreenDialog(
    title: String,
    onDismiss: () -> Unit,
    saveEnabled: Boolean,
    onSave: () -> Unit,
    saveLabel: String = "Zapisz",
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = Forest.Parchment) {
            Column(Modifier.fillMaxSize()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(Forest.Deep)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Zamknij", tint = Color.White)
                    }
                    Text(
                        title,
                        Modifier.weight(1f),
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    TextButton(onClick = onSave, enabled = saveEnabled) {
                        Text(
                            saveLabel,
                            color = if (saveEnabled) Forest.Gold else Color.White.copy(alpha = 0.4f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                        )
                    }
                }
                Column(
                    Modifier
                        .fillMaxSize()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    content = content,
                )
            }
        }
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String = "Tak",
    icon: String = "🦉",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest.Parchment,
        icon = { Text(icon, fontSize = 40.sp) },
        title = { Text(title, textAlign = TextAlign.Center) },
        text = { Text(text, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        confirmButton = { Button(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@Composable
fun PinDialog(onDismiss: () -> Unit, onSubmit: (String) -> Boolean) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest.Parchment,
        icon = { Text("🦉", fontSize = 40.sp) },
        title = { Text("Strefa rodzica") },
        text = {
            Column {
                Text("Hu-hu! Tylko rodzic może to zrobić. Wpisz PIN:")
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(8); error = false },
                    singleLine = true,
                    isError = error,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    supportingText = if (error) {
                        { Text("Niepoprawny PIN") }
                    } else null,
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (!onSubmit(pin)) {
                    error = true
                    pin = ""
                }
            }) { Text("Odblokuj") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@Composable
fun TextInputDialog(
    title: String,
    initial: String,
    label: String,
    numeric: Boolean = false,
    validate: (String) -> String? = { null },
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var value by remember { mutableStateOf(initial) }
    val error = validate(value)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest.Parchment,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = value,
                onValueChange = { value = if (numeric) it.filter(Char::isDigit).take(8) else it.take(30) },
                label = { Text(label) },
                singleLine = true,
                isError = error != null && value.isNotEmpty(),
                supportingText = error?.let { { Text(it) } },
                visualTransformation = if (numeric) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
                keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.NumberPassword else KeyboardType.Text),
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(value) }, enabled = error == null) { Text("Zapisz") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Anuluj") } },
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconPicker(selected: String, groups: List<Pair<String, List<String>>>, onSelect: (String) -> Unit) {
    var custom by remember { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        groups.forEach { (title, icons) ->
            Text(title, style = MaterialTheme.typography.labelLarge, color = Forest.Bark)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                icons.forEach { ic ->
                    val sel = ic == selected
                    val shape = RoundedCornerShape(12.dp)
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(shape)
                            .background(if (sel) Forest.Leaf.copy(alpha = 0.45f) else Color.White.copy(alpha = 0.7f))
                            .border(if (sel) 3.dp else 1.dp, if (sel) Forest.Moss else Forest.Bark.copy(alpha = 0.2f), shape)
                            .clickable { onSelect(ic) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(ic, fontSize = 26.sp)
                    }
                }
            }
        }
        OutlinedTextField(
            value = custom,
            onValueChange = {
                custom = it.take(12)
                if (it.isNotBlank()) onSelect(it.trim().take(12))
            },
            label = { Text("Własna ikonka – wpisz dowolne emoji") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
