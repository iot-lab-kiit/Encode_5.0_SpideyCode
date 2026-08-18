package `in`.iot.spidey_code.view.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import `in`.iot.spidey_code.data.model.NormalizedRect
import `in`.iot.spidey_code.vm.TransformedFaceData

/**
 * Main poster viewport container rendering camera preview, live face mask overlay,
 * and frame image overlay aligned with detected frame window coordinates.
 */
@Composable
fun CameraViewport(
    isPermissionGranted: Boolean,
    previewView: PreviewView,
    detectedFaces: List<TransformedFaceData>,
    isMaskEnabled: Boolean,
    frameImageBitmap: ImageBitmap?,
    normalizedWindow: NormalizedRect?,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(0.dp),
        contentAlignment = Alignment.TopStart
    ) {
        val containerWidth = maxWidth
        val containerHeight = maxHeight

        val window = normalizedWindow ?: NormalizedRect(0f, 0f, 1f, 1f)

        val camLeft = containerWidth * window.left
        val camTop = containerHeight * window.top
        val camWidth = containerWidth * window.width
        val camHeight = containerHeight * window.height

        if (isPermissionGranted) {
            // Layer 1 & 2: Camera Viewport & Live Mask constrained to detected frame window
            Box(
                modifier = Modifier
                    .offset(x = camLeft, y = camTop)
                    .size(width = camWidth, height = camHeight)
                    .clipToBounds()
            ) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                )

                FaceOverlayCanvas(
                    faces = detectedFaces,
                    isMaskEnabled = isMaskEnabled,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Layer 3: Selected Photobooth Frame Overlay (frame_1_2.png)
            if (frameImageBitmap != null) {
                Image(
                    bitmap = frameImageBitmap,
                    contentDescription = "Selected Frame Overlay",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            CameraPermissionNotice(
                onRequestPermission = onRequestPermission
            )
        }
    }
}
