package `in`.iot.spidey_code.view.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun FaceOverlayCanvas(
    faces: List<RectF>,
    modifier: Modifier = Modifier,
    boxColor: Color = Color(0xFF00FF66), // High-visibility neon green
    strokeWidthDp: Float = 3f
) {
    val strokeWidthPx = Stroke(width = strokeWidthDp.dp.value).width

    Canvas(modifier = modifier.fillMaxSize()) {
        for (face in faces) {
            val left = face.left
            val top = face.top
            val width = face.width()
            val height = face.height()

            // Draw Face Bounding Box Outline
            drawRect(
                color = boxColor,
                topLeft = Offset(left, top),
                size = Size(width, height),
                style = Stroke(width = strokeWidthPx)
            )
        }
    }
}
