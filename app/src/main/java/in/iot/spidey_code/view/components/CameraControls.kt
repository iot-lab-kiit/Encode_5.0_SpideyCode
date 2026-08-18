package `in`.iot.spidey_code.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.iot.spidey_code.ui.theme.SpideyRed

/**
 * Bottom control bar component housing mask toggle, shutter button with animation,
 * and camera facing switch button.
 */
@Composable
fun CameraControls(
    isMaskEnabled: Boolean,
    isCapturing: Boolean,
    onToggleMask: () -> Unit,
    onShutterClick: () -> Unit,
    onToggleCameraFacing: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = Color.Black.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Face Mask Toggle (LEFT)
            IconButton(
                onClick = onToggleMask,
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = if (isMaskEnabled) SpideyRed.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = if (isMaskEnabled) Color.White else Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isMaskEnabled) Icons.Filled.Face else Icons.Outlined.Face,
                    contentDescription = "Toggle Face Mask",
                    tint = Color.White
                )
            }

            // Camera Shutter Button (CENTER - visually dominant)
            Box(
                contentAlignment = Alignment.Center
            ) {
                CameraShutterButton(
                    isCapturing = isCapturing,
                    onClick = onShutterClick
                )

                SpideyNetAnimation(
                    modifier = Modifier.size(110.dp)
                )
            }

            // Camera Switch Button (RIGHT)
            IconButton(
                onClick = onToggleCameraFacing,
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.55f),
                        shape = CircleShape
                    )
                    .border(
                        width = 2.dp,
                        color = Color.White.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Filled.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White
                )
            }
        }
    }
}
