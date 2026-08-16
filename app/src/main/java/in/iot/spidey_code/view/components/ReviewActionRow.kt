package `in`.iot.spidey_code.view.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ReviewActionRow(
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onRetake: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReviewActionButton(
            icon = Icons.Default.Download,
            label = "DOWNLOAD",
            onClick = onDownload,
            isPrimary = false
        )

        ReviewActionButton(
            icon = Icons.Default.Share,
            label = "SHARE",
            onClick = onShare,
            isPrimary = true
        )

        ReviewActionButton(
            icon = Icons.Default.Refresh,
            label = "RETAKE",
            onClick = onRetake,
            isPrimary = false
        )
    }
}
