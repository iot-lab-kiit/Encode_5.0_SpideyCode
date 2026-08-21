package `in`.iot.spidey_code.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.iot.spidey_code.ui.theme.SpideyRed

/**
 * Bottom control bar component housing mask toggle, shutter button with animation,
 * and camera facing switch button. Laid out as three equal-weight columns (start /
 * center / end) so the shutter stays precisely centered regardless of side-button
 * sizing, matching the standard camera-app control-bar pattern.
 */
@Composable
fun CameraControls(
    isMaskEnabled: Boolean,
    isCapturing: Boolean,
    isRecording: Boolean,
    recordingProgress: Float,
    recordingLabel: String,
    onToggleMask: () -> Unit,
    onTapPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
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
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Face Mask Toggle (LEFT) -- disabled mid-recording, flipping isn't supported yet
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                IconButton(
                    onClick = onToggleMask,
                    enabled = !isRecording,
                    modifier = Modifier
                        .size(56.dp)
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
            }

            // Camera Shutter Button (CENTER - visually dominant, always exactly centered)
            // Tap = photo, press & hold = video, mirroring Snapchat's shutter gesture.
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                if (isRecording) {
                    Text(
                        text = recordingLabel,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.offset(y = (-64).dp)
                    )
                }

                CameraShutterButton(
                    isCapturing = isCapturing,
                    isRecording = isRecording,
                    recordingProgress = recordingProgress,
                    onTapPhoto = onTapPhoto,
                    onStartRecording = onStartRecording,
                    onStopRecording = onStopRecording
                )

                SpideyNetAnimation(
                    modifier = Modifier.size(110.dp)
                )
            }

            // Camera Switch Button (RIGHT) -- disabled mid-recording
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                IconButton(
                    onClick = onToggleCameraFacing,
                    enabled = !isRecording,
                    modifier = Modifier
                        .size(56.dp)
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
}
