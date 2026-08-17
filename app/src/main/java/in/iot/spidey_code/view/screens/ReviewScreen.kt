package `in`.iot.spidey_code.view.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.data.model.displayName
import `in`.iot.spidey_code.utils.saveImageToGallery
import `in`.iot.spidey_code.utils.shareImage
import `in`.iot.spidey_code.view.components.CapturedImagePreviewCard
import `in`.iot.spidey_code.view.components.ReviewActionRow
import `in`.iot.spidey_code.view.components.ReviewTopBar
import java.io.File

@Composable
fun ReviewScreen(
    selectedFilter: FilterType,
    imageUri: String?,
    onNavigateToGearSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val capturedBitmap = remember(imageUri) {
        if (imageUri != null) {
            runCatching {
                val parsedUri = Uri.parse(imageUri)
                context.contentResolver.openInputStream(parsedUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        } else {
            null
        }
    }

    val handleReturnHome = {
        if (imageUri != null) {
            runCatching {
                val parsedUri = Uri.parse(imageUri)
                if (parsedUri.scheme == "file" && parsedUri.path != null) {
                    val file = File(parsedUri.path!!)
                    if (file.exists()) {
                        file.delete()
                    }
                }
            }
        }
        onNavigateToGearSelection()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Top Bar Component
            ReviewTopBar(
                title = "REVIEW",
                filterName = selectedFilter.displayName(),
                onBack = handleReturnHome
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Constrained Image Preview Card Component
            CapturedImagePreviewCard(
                capturedBitmap = capturedBitmap,
                imageUri = imageUri,
                modifier = Modifier.weight(1f, fill = false)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Single Horizontal Action Row Component
            ReviewActionRow(
                onDownload = {
                    saveImageToGallery(context, imageUri)
                },
                onShare = {
                    shareImage(context, imageUri)
                },
                onRetake = handleReturnHome
            )
        }
    }
}
