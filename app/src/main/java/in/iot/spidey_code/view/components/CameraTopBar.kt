package `in`.iot.spidey_code.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import `in`.iot.spidey_code.vm.FlashMode

@Composable
fun CameraTopBar(
    flashMode: FlashMode,
    onCycleFlash: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTranslucentBg = Color.Black.copy(alpha = 0.65f)
    val subtleBorderColor = Color.White.copy(alpha = 0.15f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {

        // Floating Rounded-Square Flash Toggle Button (Top-Right)
        IconButton(
            onClick = onCycleFlash,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(40.dp)
                .background(
                    color = darkTranslucentBg,
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    color = subtleBorderColor,
                    shape = RoundedCornerShape(12.dp)
                )
        ) {
            Icon(
                imageVector = when (flashMode) {
                    FlashMode.OFF -> Icons.Filled.FlashOff
                    FlashMode.AUTO -> Icons.Filled.FlashAuto
                    FlashMode.ON -> Icons.Filled.FlashOn
                },
                contentDescription = "Toggle Flash",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
