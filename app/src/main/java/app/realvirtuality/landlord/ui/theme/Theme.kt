package app.realvirtuality.landlord.ui.theme

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
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

// Cam yüzey modifier'ı (yarı saydam gradyan + üst sheen + ince kenar)
fun Modifier.liquidGlass(corner: Int = 22): Modifier = this
    .clip(RoundedCornerShape(corner.dp))
    .background(
        Brush.verticalGradient(
            listOf(Color.White.copy(alpha = 0.12f), Color.White.copy(alpha = 0.03f))
        )
    )
    .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(corner.dp))

// Mercek yanması: cam üzerinde periyodik diyagonal specular sweep (iOS imza animasyonu)
@Composable
fun Modifier.specularSweep(corner: Int = 22): Modifier {
    val t = rememberInfiniteTransition(label = "spec")
    val x by t.animateFloat(
        initialValue = -0.3f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(3600, easing = LinearEasing), RepeatMode.Restart),
        label = "x")
    return this.drawWithContent {
        drawContent()
        val sx = x * size.width
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.20f), Color.Transparent),
                start = Offset(sx - 70f, 0f), end = Offset(sx + 70f, size.height)),
            cornerRadius = CornerRadius(corner.dp.toPx()))
    }
}
