package `in`.iot.spidey_code.view.components

import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import `in`.iot.spidey_code.data.model.BadgeCorner
import `in`.iot.spidey_code.data.model.NormalizedRect
import `in`.iot.spidey_code.vm.TransformedFaceData

/**
 * Main poster viewport container rendering camera preview, live face mask overlay,
 * and frame image overlay aligned with detected frame window coordinates.
 *
 * The frame is always rendered at its own native aspect ratio (derived from the
 * decoded bitmap's own pixel dimensions, not a hardcoded constant) inside a
 * best-fit box centered within the available space. This guarantees the frame
 * (and everything baked into it — logos, text, decorations) renders with the
 * exact same proportions on every device, regardless of that device's screen
 * aspect ratio; devices whose available area doesn't match the frame's aspect
 * simply get a bit of letterboxing rather than a stretched/distorted frame.
 */
@Composable
fun CameraViewport(
    isPermissionGranted: Boolean,
    previewView: PreviewView,
    detectedFaces: List<TransformedFaceData>,
    isMaskEnabled: Boolean,
    frameImageBitmap: ImageBitmap?,
    normalizedWindow: NormalizedRect?,
    badgeCorner: BadgeCorner,
    showBrandingOverlay: Boolean,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        if (isPermissionGranted) {
            val availableWidth = maxWidth
            val availableHeight = maxHeight

            val frameAspectRatio = if (frameImageBitmap != null && frameImageBitmap.height > 0) {
                frameImageBitmap.width.toFloat() / frameImageBitmap.height.toFloat()
            } else {
                9f / 16f
            }

            // Best-fit (contain) the frame's native aspect ratio within the available
            // space, matching whichever dimension is the limiting one on this device.
            val availableRatio = availableWidth.value / availableHeight.value
            val fittedWidth: Dp
            val fittedHeight: Dp
            if (availableRatio > frameAspectRatio) {
                fittedHeight = availableHeight
                fittedWidth = availableHeight * frameAspectRatio
            } else {
                fittedWidth = availableWidth
                fittedHeight = availableWidth / frameAspectRatio
            }

            val window = normalizedWindow ?: NormalizedRect(0f, 0f, 1f, 1f)

            val camLeft = fittedWidth * window.left
            val camTop = fittedHeight * window.top
            val camWidth = fittedWidth * window.width
            val camHeight = fittedHeight * window.height

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(width = fittedWidth, height = fittedHeight)
            ) {
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

                // Layer 3: Selected Photobooth Frame Overlay
                if (frameImageBitmap != null) {
                    Image(
                        bitmap = frameImageBitmap,
                        contentDescription = "Selected Frame Overlay",
                        contentScale = ContentScale.FillBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Layer 4: Shared branding (corner logos + event badge). Skipped for filters
                // whose own frame art already has branding baked in (see showBrandingOverlay).
                if (showBrandingOverlay) {
                    BrandingOverlay(
                        normalizedWindow = normalizedWindow,
                        badgeCorner = badgeCorner,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        } else {
            CameraPermissionNotice(
                onRequestPermission = onRequestPermission
            )
        }
    }
}
