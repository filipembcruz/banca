package io.github.filipembcruz.banca.ui.theme

import android.os.Build
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.filipembcruz.banca.data.settings.ThemeMode

object BancaColors {
    val Blue = Color(0xFF3B5BFD)
    val BlueLight = Color(0xFF8EA2FF)
    val Up = Color(0xFF1FA463)
    val Down = Color(0xFFE5484D)
    val UpDark = Color(0xFF4CD08A)
    val DownDark = Color(0xFFFF6B6F)
}

private val Light = lightColorScheme(
    primary = BancaColors.Blue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE3E8FF),
    onPrimaryContainer = Color(0xFF0E1F7A),
    secondary = Color(0xFF5B6275),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE9ECF4),
    onSecondaryContainer = Color(0xFF1B2030),
    tertiary = Color(0xFF8A5CF6),
    background = Color(0xFFF6F7FB),
    onBackground = Color(0xFF12151C),
    surface = Color(0xFFF6F7FB),
    onSurface = Color(0xFF12151C),
    surfaceVariant = Color(0xFFEDEFF5),
    onSurfaceVariant = Color(0xFF5B6275),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF0F2F8),
    surfaceContainerHigh = Color(0xFFEAEDF4),
    surfaceContainerHighest = Color(0xFFE3E6EE),
    outline = Color(0xFFCBD0DC),
    outlineVariant = Color(0xFFE2E5EC),
    error = Color(0xFFD93A3F),
)

private val Dark = darkColorScheme(
    primary = BancaColors.BlueLight,
    onPrimary = Color(0xFF0A1550),
    primaryContainer = Color(0xFF26368F),
    onPrimaryContainer = Color(0xFFDDE3FF),
    secondary = Color(0xFFA9B0C2),
    onSecondary = Color(0xFF1A1F2B),
    secondaryContainer = Color(0xFF262C3A),
    onSecondaryContainer = Color(0xFFDDE1EC),
    tertiary = Color(0xFFB79BFF),
    background = Color(0xFF12151C),
    onBackground = Color(0xFFE7E9F0),
    surface = Color(0xFF12151C),
    onSurface = Color(0xFFE7E9F0),
    surfaceVariant = Color(0xFF232836),
    onSurfaceVariant = Color(0xFF9AA2B5),
    surfaceContainerLowest = Color(0xFF0D1015),
    surfaceContainerLow = Color(0xFF181C25),
    surfaceContainer = Color(0xFF1C212B),
    surfaceContainerHigh = Color(0xFF232836),
    surfaceContainerHighest = Color(0xFF2B3140),
    outline = Color(0xFF3A4152),
    outlineVariant = Color(0xFF2A3040),
    error = Color(0xFFFF6B6F),
)

private fun ColorScheme.amoled() = copy(
    background = Color.Black,
    surface = Color.Black,
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0B0D12),
    surfaceContainer = Color(0xFF101319),
    surfaceContainerHigh = Color(0xFF161A22),
)

private val AppTypography = Typography().let { t ->
    t.copy(
        displaySmall = t.displaySmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.5).sp),
        headlineLarge = t.headlineLarge.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.5).sp),
        headlineMedium = t.headlineMedium.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp),
        headlineSmall = t.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleLarge = t.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.2).sp),
        titleMedium = t.titleMedium.copy(fontWeight = FontWeight.SemiBold, lineHeight = 22.sp),
        titleSmall = t.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = t.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    )
}

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Immutable
data class Extras(
    val dark: Boolean,
    val up: Color,
    val down: Color,
    val card: Color,
    val reducedMotion: Boolean,
)

val LocalExtras = staticCompositionLocalOf {
    Extras(dark = false, up = BancaColors.Up, down = BancaColors.Down, card = Color.White, reducedMotion = false)
}

/** Reader text style, from Settings. */
fun readerStyle(serif: Boolean, scale: Float, color: Color) = TextStyle(
    fontFamily = if (serif) FontFamily.Serif else FontFamily.Default,
    fontSize = (18 * scale).sp,
    lineHeight = (18 * scale * 1.62f).sp,
    color = color,
)

@Composable
fun BancaTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    amoled: Boolean = false,
    dynamic: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    var scheme = when {
        dynamic && Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    if (dark && amoled) scheme = scheme.amoled()
    val extras = Extras(
        dark = dark,
        up = if (dark) BancaColors.UpDark else BancaColors.Up,
        down = if (dark) BancaColors.DownDark else BancaColors.Down,
        card = if (dark) scheme.surfaceContainerLow else Color.White,
        reducedMotion = reducedMotion,
    )
    CompositionLocalProvider(LocalExtras provides extras) {
        MaterialTheme(colorScheme = scheme, typography = AppTypography, shapes = AppShapes) {
            // Text outside a Surface would otherwise default to black, unreadable in dark mode.
            CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides scheme.onBackground, content = content)
        }
    }
}

/** Spring used across the app; snaps when the user asked for reduced motion. */
object Motion {
    fun <T> bouncy(reduced: Boolean, stiffness: Float = Spring.StiffnessMediumLow): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = stiffness)

    fun <T> smooth(reduced: Boolean): FiniteAnimationSpec<T> =
        if (reduced) snap() else spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    val offsetSpring = spring<IntOffset>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}
