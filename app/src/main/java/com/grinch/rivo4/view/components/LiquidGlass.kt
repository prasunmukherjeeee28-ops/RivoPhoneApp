package com.grinch.rivo4.view.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.grinch.rivo4.controller.util.LiquidGlassSettings
import com.grinch.rivo4.controller.util.PreferenceManager
import org.koin.compose.koinInject

/**
 * Applies the configurable translucent crystal sheen and reflective edge to a Compose surface.
 *
 * The preference snapshot is collected here so callers can simply use
 * Modifier.liquidGlassEffect() without threading settings through every screen.
 *
 * Note: Compose's blur modifier blurs the node it is attached to (including text/icons).
 * The configurable blur is therefore applied to the isolated mesh backdrop below, keeping
 * foreground content sharp while still producing the frosted-glass look.
 */
@Composable
fun Modifier.liquidGlassEffect(
    shape: Shape = RoundedCornerShape(24.dp)
): Modifier {
    val prefs = koinInject<PreferenceManager>()
    val settings by prefs.liquidGlassSettings.collectAsState()

    if (!settings.isLiquidGlassEnabled) return this

    val transparency = settings.glassTransparency.coerceIn(0.05f, 0.60f)
    val reflectiveAlpha = (transparency + 0.10f).coerceIn(0f, 1f)

    return this
        .clip(shape)
        .background(
            color = Color.White.copy(alpha = transparency),
            shape = shape
        )
        .border(
            width = 0.8.dp,
            color = Color.White.copy(alpha = reflectiveAlpha),
            shape = shape
        )
}

/**
 * Add this as the first child of a Box to render the optional animated mesh behind UI.
 * When the feature or mesh option is disabled, it draws nothing and starts no animation.
 */
@Composable
fun LiquidGlassMeshBackground(
    modifier: Modifier = Modifier
) {
    val prefs = koinInject<PreferenceManager>()
    val settings by prefs.liquidGlassSettings.collectAsState()

    if (settings.isLiquidGlassEnabled && settings.isDynamicMeshEnabled) {
        LiquidGlassGradientMesh(
            modifier = modifier,
            settings = settings
        )
    }
}

@Composable
private fun LiquidGlassGradientMesh(
    modifier: Modifier,
    settings: LiquidGlassSettings
) {
    val transition = rememberInfiniteTransition(label = "LiquidGlassMesh")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 28000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LiquidGlassMeshPhase"
    )
    val secondaryPhase by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 36000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LiquidGlassMeshSecondaryPhase"
    )

    // Three soft gradient fields are much cheaper than per-frame image shaders or particle systems.
    // Blur only this background canvas, not the actual panel content.
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .blur(
                radius = settings.glassBlurRadius.coerceIn(0f, 30f).dp,
                edgeTreatment = BlurredEdgeTreatment.Unbounded
            )
    ) {
        val width = size.width
        val height = size.height
        val colors = listOf(
            Color(0xFF83BDF7),
            Color(0xFFB9A7F4),
            Color(0xFF8BE0D4),
            Color(0xFFF4C7D9)
        )
        drawRect(
            brush = Brush.linearGradient(
                colors = colors,
                start = Offset(-width * 0.25f + width * 0.35f * phase, -height * 0.10f),
                end = Offset(width * 0.95f - width * 0.2f * phase, height * 1.05f)
            )
        )

        val firstCenter = Offset(
            x = width * (0.18f + 0.30f * phase),
            y = height * (0.20f + 0.22f * secondaryPhase)
        )
        val firstRadius = maxOf(width, height) * 0.56f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFF7DD3FC).copy(alpha = 0.72f), Color.Transparent),
                center = firstCenter,
                radius = firstRadius
            ),
            radius = firstRadius,
            center = firstCenter
        )

        val secondCenter = Offset(
            x = width * (0.82f - 0.28f * secondaryPhase),
            y = height * (0.76f - 0.25f * phase)
        )
        val secondRadius = maxOf(width, height) * 0.50f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFC4B5FD).copy(alpha = 0.68f), Color.Transparent),
                center = secondCenter,
                radius = secondRadius
            ),
            radius = secondRadius,
            center = secondCenter
        )
    }
}
