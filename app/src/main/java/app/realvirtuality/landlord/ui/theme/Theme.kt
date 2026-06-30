package app.realvirtuality.landlord.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Liquid-Glass esinli koyu tema (iOS Hooder ile aynı palet)
object Brand {
    val bg = Color(0xFF04080F)
    val surface = Color(0xFF0A0E1A)
    val primary = Color(0xFF3494FF)
    val gold = Color(0xFFFFC434)
    val green = Color(0xFF30D158)
    val orange = Color(0xFFFF9F0A)
    val text = Color(0xFFF2F5FA)
    val textSub = Color(0xFFAEB6C6)
    val textMuted = Color(0xFF6B7689)
}

private val DarkScheme = darkColorScheme(
    primary = Brand.primary,
    background = Brand.bg,
    surface = Brand.surface,
    onPrimary = Color.White,
    onBackground = Brand.text,
    onSurface = Brand.text,
)

@Composable
fun HooderTheme(content: @Composable () -> Unit) =
    MaterialTheme(colorScheme = DarkScheme, content = content)

// Cam yüzey modifier'ı (yarı saydam gradyan + ince kenar)
fun Modifier.liquidGlass(corner: Int = 22): Modifier = this
    .clip(RoundedCornerShape(corner.dp))
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.03f))
        )
    )
    .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(corner.dp))
