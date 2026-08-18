package `in`.iot.spidey_code.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import `in`.iot.spidey_code.ui.theme.SpiderBorderBlack
import `in`.iot.spidey_code.ui.theme.SpiderSurfaceContainerHigh

/**
 * Bottom action row on the Review screen: Download / Share / Retake, rendered
 * as three equal-weight buttons so Share (the primary action) stands out only
 * through color, not through mismatched sizing.
 */
@Composable
fun ReviewActionRow(
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        BrutalistBox(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 384.dp),
            shape = RoundedCornerShape(12.dp),
            bg = SpiderSurfaceContainerHigh,
            borderColor = SpiderBorderBlack,
            borderWidth = 4.dp,
            shadowOffset = 4.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReviewActionButton(
                    icon = Icons.Filled.Download,
                    label = "DOWNLOAD",
                    onClick = onDownload
                )

                ReviewActionButton(
                    icon = Icons.Filled.Share,
                    label = "SHARE",
                    onClick = onShare,
                    isPrimary = true
                )

                ReviewActionButton(
                    icon = Icons.Filled.Refresh,
                    label = "RETAKE",
                    onClick = onRetake
                )
            }
        }
    }
}
