package com.sutikshan.scorecore.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sutikshan.scorecore.engine.BallView
import com.sutikshan.scorecore.engine.ExtraType

/** Text with the app's defaults, so call sites stay short. */
@Composable
fun Label(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = IosType.body,
    color: Color = Ios.Label,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/**
 * iOS-style press feedback: no ripple, just a quick fade. Put this *before* clip/background in the
 * modifier chain so the whole element fades, not only its content.
 */
@Composable
fun Modifier.iosClickable(enabled: Boolean = true, onClick: () -> Unit): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.55f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "press",
    )
    return this
        .alpha(alpha)
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

@Composable
fun NavBar(
    title: String,
    backLabel: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit = {},
) {
    Box(
        modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(44.dp)
            .padding(horizontal = 8.dp),
    ) {
        if (backLabel != null) {
            Row(
                Modifier
                    .align(Alignment.CenterStart)
                    .iosClickable(onClick = onBack)
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label("‹", style = IosType.chevron, color = Ios.Blue)
                Spacer(Modifier.width(5.dp))
                Label(backLabel, style = IosType.body, color = Ios.Blue)
            }
        }
        Label(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            style = IosType.headline,
            maxLines = 1,
        )
        Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

@Composable
fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Label(
        text = text.uppercase(),
        modifier = modifier.padding(start = 16.dp, top = 22.dp, bottom = 7.dp),
        style = IosType.footnote,
        color = Ios.LabelSecondary,
    )
}

@Composable
fun GroupCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Ios.Surface),
        content = content,
    )
}

@Composable
fun RowDivider(inset: Dp = 16.dp) {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = inset)
            .height(0.5.dp)
            .background(Ios.Separator),
    )
}

@Composable
fun ValueRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = Ios.LabelSecondary,
    onClick: (() -> Unit)? = null,
) {
    val base = modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
    val clickable = if (onClick != null) base.iosClickable(onClick = onClick) else base
    Row(
        clickable.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Label(title, Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Label(value, color = valueColor, textAlign = TextAlign.End, maxLines = 2)
        if (onClick != null) {
            Spacer(Modifier.width(8.dp))
            Label("›", style = IosType.chevron, color = Ios.LabelTertiary)
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .iosClickable(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(if (enabled) Ios.Blue else Ios.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, style = IosType.headline, color = if (enabled) Color.White else Ios.LabelTertiary)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Ios.Blue) {
    Box(
        modifier
            .fillMaxWidth()
            .height(48.dp)
            .iosClickable(onClick = onClick)
            .clip(RoundedCornerShape(14.dp))
            .background(Ios.SurfaceHigh),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, style = IosType.headline, color = color)
    }
}

@Composable
fun SegmentedControl(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Ios.Surface)
            .padding(2.dp),
    ) {
        val segmentWidth = maxWidth / options.size
        val thumbOffset by animateDpAsState(
            targetValue = segmentWidth * selected,
            animationSpec = spring(dampingRatio = 0.85f, stiffness = 500f),
            label = "segment",
        )
        Box(
            Modifier
                .offset(x = thumbOffset)
                .width(segmentWidth)
                .fillMaxHeight()
                .clip(RoundedCornerShape(8.dp))
                .background(Ios.Blue),
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .iosClickable { onSelect(index) },
                    contentAlignment = Alignment.Center,
                ) {
                    Label(
                        text = option,
                        style = IosType.footnoteSemibold,
                        color = if (index == selected) Color.White else Ios.LabelSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

@Composable
fun Pill(text: String, background: Color = Ios.Blue.copy(alpha = 0.2f), foreground: Color = Ios.Blue) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(background)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label(text, style = IosType.caption, color = foreground, maxLines = 1)
    }
}

/** The little coloured circle for one delivery (0, 1, 4, 6, W, Wd ...). */
@Composable
fun BallChip(view: BallView, size: Dp = 34.dp) {
    val b = view.ball
    val colors: Pair<Color, Color> = when {
        b.isWicket -> Ios.Red to Color.White
        b.extra != ExtraType.NONE -> Ios.Orange.copy(alpha = 0.22f) to Ios.Orange
        b.batRuns == 6 -> Ios.Indigo to Color.White
        b.batRuns == 4 -> Ios.Blue to Color.White
        b.batRuns == 0 -> Ios.SurfaceHigh to Ios.LabelSecondary
        else -> Ios.SurfaceHigh to Ios.Label
    }
    val text = view.chip
    Box(
        Modifier
            .height(size)
            .defaultMinSize(minWidth = size)
            .clip(if (text.length > 2) RoundedCornerShape(50) else CircleShape)
            .background(colors.first)
            .padding(horizontal = if (text.length > 2) 8.dp else 0.dp),
        contentAlignment = Alignment.Center,
    ) {
        Label(
            text = text,
            style = if (text.length > 2) IosType.caption else IosType.footnoteSemibold,
            color = colors.second,
            maxLines = 1,
        )
    }
}

/** A centred, iOS-alert-like dialog container. */
@Composable
fun IosDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            Modifier
                .padding(horizontal = 28.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Ios.SurfaceHigh)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Label(title, style = IosType.headline, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            content()
        }
    }
}
