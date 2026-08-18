package `in`.iot.spidey_code.view.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import `in`.iot.spidey_code.ui.theme.SpiderBorderBlack
import `in`.iot.spidey_code.ui.theme.SpiderSurfaceContainerHigh
import kotlin.math.tan

fun Modifier.skewX(deg: Float): Modifier = drawWithContent {
    drawIntoCanvas { canvas ->
        canvas.save()
        canvas.translate(size.width / 2f, size.height / 2f)
        canvas.nativeCanvas.skew(tan(Math.toRadians(deg.toDouble())).toFloat(), 0f)
        canvas.translate(-size.width / 2f, -size.height / 2f)
        drawContent()
        canvas.restore()
    }
}

@Composable
fun BrutalistBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(0.dp),
    bg: Color = SpiderSurfaceContainerHigh,
    borderColor: Color = SpiderBorderBlack,
    borderWidth: Dp = 4.dp,
    shadowOffset: Dp = 4.dp,
    contentAlignment: Alignment = Alignment.Center,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val animTranslation by animateDpAsState(
        targetValue = if (pressed && onClick != null) 2.dp else 0.dp,
        animationSpec = tween(100),
        label = "brutalistPress"
    )

    Box(
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable(source, null, onClick = onClick) else Modifier
        ),
        propagateMinConstraints = true
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .offset(shadowOffset, shadowOffset)
                .background(SpiderBorderBlack, shape)
        )
        Box(
            modifier = Modifier
                .offset(animTranslation, animTranslation)
                .background(bg, shape)
                .border(borderWidth, borderColor, shape)
                .clip(shape),
            contentAlignment = contentAlignment,
            content = content
        )
    }
}
