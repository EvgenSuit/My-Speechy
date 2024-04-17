package com.example.myspeechy.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp



val itemBackgroundGradient = Brush.linearGradient(
    colorStops = arrayOf(
        0f to Color.Blue.copy(alpha = 0.8f),
        0.4f to Color.Blue.copy(alpha = 0.7f),
        1f to Color.Magenta.copy(alpha = 0.7f)
    )
)
val meditationStatsBackgroundGradient = Brush.linearGradient(
    colorStops = arrayOf(
        0f to Color.Red,
        0.7f to Color.Blue
    )
)

val readingItemButtonBarGradient = Brush.linearGradient(
    colorStops = arrayOf(
        0f to Color.Magenta.copy(alpha = 0.8f),
        0.5f to Color.Blue.copy(alpha = 0.7f),
        1f to Color.Cyan
    )
)

fun Modifier.advancedShadow(
    color: Color = Color.Black,
    alpha: Float = 1f,
    cornersRadius: Dp = 0.dp,
    shadowBlurRadius: Dp = 0.dp,
    offsetY: Dp = 0.dp,
    offsetX: Dp = 0.dp
) = drawBehind {

    val shadowColor = color.copy(alpha = alpha).toArgb()
    val transparentColor = color.copy(alpha = 0f).toArgb()

    drawIntoCanvas {
        val paint = Paint()
        val frameworkPaint = paint.asFrameworkPaint()
        frameworkPaint.color = transparentColor
        frameworkPaint.setShadowLayer(
            shadowBlurRadius.toPx(),
            offsetX.toPx(),
            offsetY.toPx(),
            shadowColor
        )
        it.drawRoundRect(
            0f,
            0f,
            this.size.width,
            this.size.height,
            cornersRadius.toPx(),
            cornersRadius.toPx(),
            paint
        )
    }
}