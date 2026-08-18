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
    isMaskEnabled: Boolean = false,
    modifier: Modifier = Modifier,
    showDebugBoundingBox: Boolean = false
) {
    val context = LocalContext.current

    // Cache the PNG mask bitmap once so it is not decoded on every camera frame
    val maskImageBitmap: ImageBitmap? = remember(context) {
        try {
            context.assets.open("masks/spidey_mask.png").use { inputStream ->
                val options = BitmapFactory.Options().apply {
                    inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888
                    inPremultiplied = true
                }
                BitmapFactory.decodeStream(inputStream, null, options)?.asImageBitmap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    if (!isMaskEnabled || faces.isEmpty()) return

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

                val scaleFactor = box.width() / SpideyMaskReference.referenceMaskFaceWidth
                val deltaAngleDeg = detectedEyeAngleDeg - SpideyMaskReference.maskEyeAngleDeg

                android.util.Log.d(
                    "MaskTransform",
                    "LIVE: canvasSize=${size.width}x${size.height}, boxWidth=${box.width()}, eyeCenter=$detectedEyeCenter, scaleFactor=$scaleFactor, deltaAngle=$deltaAngleDeg"
                )

                withTransform({
                    translate(left = detectedEyeCenter.x, top = detectedEyeCenter.y)
                    rotate(degrees = deltaAngleDeg, pivot = Offset.Zero)
                    scale(scaleX = scaleFactor, scaleY = scaleFactor, pivot = Offset.Zero)
                }) {
                    drawImage(
                        image = maskImageBitmap,
                        topLeft = Offset(-SpideyMaskReference.maskEyeCenter.x, -SpideyMaskReference.maskEyeCenter.y)
                    )
                }
            }

            // 2. Debug Overlay (strictly disabled by default)
            if (showDebugBoundingBox) {
                drawRect(
                    color = Color.Red,
                    topLeft = Offset(box.left, box.top),
                    size = Size(box.width(), box.height()),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}




