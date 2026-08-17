package `in`.iot.spidey_code.view.components

import android.graphics.BitmapFactory
import android.graphics.PointF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.iot.spidey_code.vm.TransformedFaceData
import kotlin.math.atan2

/**
 * Explicit reference eye landmarks and face proportions for spidey_mask.png (1024 x 1381 px).
 * MASK_LEFT_EYE: Lens center on the left side of the PNG bitmap (subject's left eye in mirror view).
 * MASK_RIGHT_EYE: Lens center on the right side of the PNG bitmap (subject's right eye in mirror view).
 */
object SpideyMaskReference {
    const val MASK_WIDTH = 1024f
    const val MASK_HEIGHT = 1381f

    // Configurable reference ratio: fraction of PNG width representing the usable mask face width
    const val MASK_FACE_WIDTH_RATIO = 0.90f
    val referenceMaskFaceWidth = MASK_WIDTH * MASK_FACE_WIDTH_RATIO // ~921.6f px

    val MASK_LEFT_EYE = PointF(285.4f, 748.4f)
    val MASK_RIGHT_EYE = PointF(755.2f, 773.6f)

    val maskEyeCenter = PointF(
        (MASK_LEFT_EYE.x + MASK_RIGHT_EYE.x) / 2f,
        (MASK_LEFT_EYE.y + MASK_RIGHT_EYE.y) / 2f
    )

    val dx = MASK_RIGHT_EYE.x - MASK_LEFT_EYE.x
    val dy = MASK_RIGHT_EYE.y - MASK_LEFT_EYE.y

    val maskEyeAngleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat() // ~3.07 deg
}

@Composable
fun FaceOverlayCanvas(
    faces: List<TransformedFaceData>,
    modifier: Modifier = Modifier,
    boxColor: Color = Color(0xFF00FF66), // High-visibility neon green
    strokeWidthDp: Float = 3f,
    showDebugBoundingBox: Boolean = true
) {
    val context = LocalContext.current
    val strokeWidthPx = Stroke(width = strokeWidthDp.dp.value).width

    // Cache the PNG mask bitmap once so it is not decoded on every camera frame
    val maskImageBitmap: ImageBitmap? = remember(context) {
        try {
            context.assets.open("masks/spidey_mask.png").use { inputStream ->
                BitmapFactory.decodeStream(inputStream)?.asImageBitmap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        for (faceData in faces) {
            val box = faceData.boundingBox
            val leftEye = faceData.leftEye
            val rightEye = faceData.rightEye

            // 1. Render Transparent PNG Face Mask
            // Position: Eye midpoint | Rotation: Eye-line angle | Scale: Bounding box face width
            if (maskImageBitmap != null && leftEye != null && rightEye != null) {
                val detectedEyeCenter = PointF(
                    (leftEye.x + rightEye.x) / 2f,
                    (leftEye.y + rightEye.y) / 2f
                )

                val dx = rightEye.x - leftEye.x
                val dy = rightEye.y - leftEye.y

                val detectedEyeAngleDeg = Math.toDegrees(atan2(dy, dx).toDouble()).toFloat()

                // Scale derived exclusively from transformed face bounding box width
                val scaleFactor = box.width() / SpideyMaskReference.referenceMaskFaceWidth

                // Rotation delta derived exclusively from eye-line angle
                val deltaAngleDeg = detectedEyeAngleDeg - SpideyMaskReference.maskEyeAngleDeg

                // Matrix Transformation Pipeline:
                // 1. Move maskEyeCenter to local origin (0, 0)
                // 2. Scale & vertically orient (scaleX, -scaleY) around (0, 0)
                // 3. Rotate around (0, 0) according to eye-line tilt
                // 4. Translate (0, 0) to detectedEyeCenter on screen
                withTransform({
                    translate(left = detectedEyeCenter.x, top = detectedEyeCenter.y)
                    rotate(degrees = deltaAngleDeg, pivot = Offset.Zero)
                    scale(scaleX = scaleFactor, scaleY = -scaleFactor, pivot = Offset.Zero)
                }) {
                    drawImage(
                        image = maskImageBitmap,
                        topLeft = Offset(-SpideyMaskReference.maskEyeCenter.x, -SpideyMaskReference.maskEyeCenter.y)
                    )
                }
            }

            // 2. Debug Overlay: Face Bounding Box Outline (Debug only)
            if (showDebugBoundingBox) {
                drawRect(
                    color = boxColor,
                    topLeft = Offset(box.left, box.top),
                    size = Size(box.width(), box.height()),
                    style = Stroke(width = strokeWidthPx)
                )
            }
        }
    }
}




