package com.joshua.glasscalc.ui.theme

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Full-screen backdrop: a deep gradient with a few large, soft, blurred colour
 * "planes" drifting behind everything. This is what the glass panels above it
 * refract — without light behind the glass, translucency reads as flat grey.
 */
@Composable
fun LiquidGlassBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(BgTop, BgMid, BgBottom))
            )
    ) {
        // Floating colour planes — purely decorative light sources for the glass to catch.
        GlassPlaneGlow(color = AccentPurple, size = 260.dp, x = (-60).dp, y = 40.dp)
        GlassPlaneGlow(color = AccentBlue, size = 220.dp, x = 240.dp, y = 520.dp)
        GlassPlaneGlow(color = AccentOrange, size = 180.dp, x = (-30).dp, y = 760.dp)
    }
}

@Composable
private fun GlassPlaneGlow(color: Color, size: androidx.compose.ui.unit.Dp, x: androidx.compose.ui.unit.Dp, y: androidx.compose.ui.unit.Dp) {
    Box(
        modifier = Modifier
            .offset(x = x, y = y)
            .size(size)
            .blur(radius = 90.dp)
            .background(color.copy(alpha = 0.35f), CircleShape)
    )
}

/**
 * A single "pane" of liquid glass: translucent fill, blurred backdrop sample,
 * and a bright top-left / dim bottom-right border that sells the refraction.
 */
@Composable
fun GlassPane(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(28.dp),
    fillAlphaTop: Float = 0.20f,
    fillAlphaBottom: Float = 0.06f,
    content: @Composable Box.() -> Unit
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.White.copy(alpha = fillAlphaTop),
                        Color.White.copy(alpha = fillAlphaBottom)
                    )
                ),
                shape
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(GlassBorderLight, GlassBorderDark),
                    start = Offset(0f, 0f),
                    end = Offset(300f, 300f)
                ),
                shape = shape
            )
    ) {
        content()
    }
}

/**
 * A tappable glass "pill" — used for calculator buttons. Presses cause a
 * gentle scale-down + brightness lift, mimicking Liquid Glass's squish.
 */
@Composable
fun GlassButton(
    label: String,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    fontSize: androidx.compose.ui.unit.TextUnit = 28.sp,
    shape: Shape = CircleShape,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = spring(dampingRatio = 0.45f, stiffness = 400f),
        label = "glassButtonScale"
    )

    val baseTop = tint?.copy(alpha = 0.55f) ?: Color.White.copy(alpha = if (pressed) 0.30f else 0.18f)
    val baseBottom = tint?.copy(alpha = 0.30f) ?: Color.White.copy(alpha = if (pressed) 0.14f else 0.05f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(Brush.verticalGradient(listOf(baseTop, baseBottom)), shape)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(GlassBorderLight, GlassBorderDark),
                    start = Offset(0f, 0f),
                    end = Offset(200f, 200f)
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Text(
            text = label,
            color = TextPrimary,
            fontSize = fontSize,
            textAlign = TextAlign.Center
        )
    }
}
