package `in`.iot.spidey_code.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.iot.spidey_code.ui.theme.SpiderBorderBlack
import `in`.iot.spidey_code.ui.theme.SpiderOnPrimaryContainer
import `in`.iot.spidey_code.ui.theme.SpiderPrimaryContainer
import `in`.iot.spidey_code.ui.theme.SpiderSurfaceContainerHigh

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
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ReviewActionButton(
                    icon = Icons.Filled.Download,
                    label = "DOWNLOAD",
                    onClick = onDownload
                )

                BrutalistBox(
                    modifier = Modifier.skewX(-6f),
                    shape = RoundedCornerShape(12.dp),
                    bg = SpiderPrimaryContainer,
                    borderColor = SpiderBorderBlack,
                    borderWidth = 4.dp,
                    shadowOffset = 4.dp,
                    onClick = onShare
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share",
                            tint = SpiderOnPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "SHARE",
                            color = SpiderOnPrimaryContainer,
                            fontSize = 24.sp,
                            lineHeight = 28.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.skewX(6f)
                        )
                    }
                }

                ReviewActionButton(
                    icon = Icons.Filled.Refresh,
                    label = "RETAKE",
                    onClick = onRetake
                )
            }
        }
    }
}
