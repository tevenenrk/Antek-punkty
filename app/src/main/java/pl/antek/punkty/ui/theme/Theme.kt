package pl.antek.punkty.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object Forest {
    val Deep = Color(0xFF1E4D2B)
    val Moss = Color(0xFF4F7942)
    val Leaf = Color(0xFF8BC34A)
    val LeafTint = Color(0xFFE8F5D0)
    val Parchment = Color(0xFFFFF8E7)
    val Bark = Color(0xFF6D4C41)
    val BarkDark = Color(0xFF4E342E)
    val BarkLight = Color(0xFF8D6E63)
    val Mushroom = Color(0xFFD84343)
    val MushroomTint = Color(0xFFFBE3E0)
    val Gold = Color(0xFFF2B705)
    val Ink = Color(0xFF2B2B1F)
}

private val scheme = lightColorScheme(
    primary = Forest.Moss,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDCEDC8),
    onPrimaryContainer = Forest.Deep,
    secondary = Forest.Bark,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE0CF),
    onSecondaryContainer = Forest.BarkDark,
    tertiary = Forest.Gold,
    onTertiary = Forest.Ink,
    background = Forest.Parchment,
    onBackground = Forest.Ink,
    surface = Forest.Parchment,
    onSurface = Forest.Ink,
    surfaceVariant = Color(0xFFF1E8D2),
    onSurfaceVariant = Forest.BarkDark,
    error = Forest.Mushroom,
    outline = Forest.BarkLight,
)

private val base = Typography()
private val typography = base.copy(
    headlineSmall = base.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
    headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
    titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Bold),
)

@Composable
fun ForestTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        typography = typography,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(18.dp),
            large = RoundedCornerShape(24.dp),
        ),
        content = content,
    )
}
