package com.sutikshan.scorecore.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.sutikshan.scorecore.R

/** iOS dark-mode palette on a true-black (AMOLED) canvas with solid system blues. */
object Ios {
    val Black = Color(0xFF000000)
    val Surface = Color(0xFF1C1C1E)
    val SurfaceHigh = Color(0xFF2C2C2E)
    val Separator = Color(0xFF38383A)

    val Label = Color(0xFFFFFFFF)
    val LabelSecondary = Color(0xFF8E8E93)
    val LabelTertiary = Color(0xFF5C5C61)

    val Blue = Color(0xFF0A84FF)
    val Indigo = Color(0xFF5E5CE6)
    val Red = Color(0xFFFF453A)
    val Orange = Color(0xFFFF9F0A)
}

/** Inter is the closest freely licensed match to SF Pro. */
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

private const val TABULAR = "tnum"

object IosType {
    val largeTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 34.sp, letterSpacing = (-0.6).sp)
    val title3 = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 20.sp, letterSpacing = (-0.3).sp, fontFeatureSettings = TABULAR)
    val headline = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 17.sp, letterSpacing = (-0.3).sp)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 17.sp, letterSpacing = (-0.3).sp, fontFeatureSettings = TABULAR)
    val subhead = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, letterSpacing = (-0.2).sp, fontFeatureSettings = TABULAR)
    val subheadSemibold = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = (-0.2).sp, fontFeatureSettings = TABULAR)
    val footnote = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 13.sp, letterSpacing = (-0.1).sp, fontFeatureSettings = TABULAR)
    val footnoteSemibold = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, letterSpacing = (-0.1).sp, fontFeatureSettings = TABULAR)
    val caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, letterSpacing = 0.1.sp, fontFeatureSettings = TABULAR)
    val chevron = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 26.sp)
    val keypad = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.4).sp, fontFeatureSettings = TABULAR)
    val score = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 60.sp, letterSpacing = (-1.8).sp, fontFeatureSettings = TABULAR)
}

private val ScoreColors = darkColorScheme(
    primary = Ios.Blue,
    onPrimary = Color.White,
    background = Ios.Black,
    onBackground = Ios.Label,
    surface = Ios.Black,
    onSurface = Ios.Label,
    surfaceVariant = Ios.Surface,
    onSurfaceVariant = Ios.LabelSecondary,
    outline = Ios.Separator,
)

private val ScoreTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = Inter),
        displayMedium = base.displayMedium.copy(fontFamily = Inter),
        displaySmall = base.displaySmall.copy(fontFamily = Inter),
        headlineLarge = base.headlineLarge.copy(fontFamily = Inter),
        headlineMedium = base.headlineMedium.copy(fontFamily = Inter),
        headlineSmall = base.headlineSmall.copy(fontFamily = Inter),
        titleLarge = base.titleLarge.copy(fontFamily = Inter),
        titleMedium = base.titleMedium.copy(fontFamily = Inter),
        titleSmall = base.titleSmall.copy(fontFamily = Inter),
        bodyLarge = base.bodyLarge.copy(fontFamily = Inter),
        bodyMedium = base.bodyMedium.copy(fontFamily = Inter),
        bodySmall = base.bodySmall.copy(fontFamily = Inter),
        labelLarge = base.labelLarge.copy(fontFamily = Inter),
        labelMedium = base.labelMedium.copy(fontFamily = Inter),
        labelSmall = base.labelSmall.copy(fontFamily = Inter),
    )
}

@Composable
fun ScoreCoreTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ScoreColors, typography = ScoreTypography) {
        Surface(color = Ios.Black, contentColor = Ios.Label, content = content)
    }
}
