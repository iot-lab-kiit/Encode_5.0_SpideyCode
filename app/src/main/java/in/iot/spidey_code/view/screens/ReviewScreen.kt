package `in`.iot.spidey_code.view.screens

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.data.model.displayName
import `in`.iot.spidey_code.ui.theme.SpiderSurface
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

    val backgroundBitmap = remember {
        runCatching {
            context.assets.open("images/spider_verse_bg.jpg").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    val capturedBitmap = remember(imageUri) {
        if (imageUri != null) {
            runCatching {
                val parsedUri = Uri.parse(imageUri)
                context.contentResolver.openInputStream(parsedUri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        } else null
    }

    val handleReturnHome = {
        if (imageUri != null) {
            runCatching {
                val parsedUri = Uri.parse(imageUri)
                if (parsedUri.scheme == "file" && parsedUri.path != null) {
                    val file = File(parsedUri.path!!)
                    if (file.exists()) file.delete()
                }
            }
        }
        onNavigateToGearSelection()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpiderSurface)
    ) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                colorFilter = ColorFilter.tint(
                    SpiderSurface,
                    BlendMode.Multiply
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val step = 20.dp.toPx()
            for (x in 0..(size.width / step).toInt()) {
                for (y in 0..(size.height / step).toInt()) {
                    drawCircle(Color(0x0DFFFFFF), 1.dp.toPx(), Offset(x * step, y * step))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top Bar
            ReviewTopBar(
                filterName = selectedFilter.displayName(),
                onBack = handleReturnHome
            )

            // Main Poster Display Area: Occupies full available space between top bar & bottom action row
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                val isReady = capturedBitmap != null
                val previewAlpha by animateFloatAsState(
                    targetValue = if (isReady) 1f else 0f,
                    animationSpec = tween(320),
                    label = "previewAlpha"
                )
                val previewScale by animateFloatAsState(
                    targetValue = if (isReady) 1f else 0.92f,
                    animationSpec = tween(320),
                    label = "previewScale"
                )

                CapturedImagePreviewCard(
                    capturedBitmap = capturedBitmap,
                    imageUri = imageUri,
                    modifier = Modifier.graphicsLayer {
                        alpha = if (isReady) previewAlpha else 1f
                        scaleX = if (isReady) previewScale else 1f
                        scaleY = if (isReady) previewScale else 1f
                    }
                )
            }

            // Bottom Control Action Row (Download / Share / Retake)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ReviewActionRow(
                        onDownload = { saveImageToGallery(context, imageUri) },
                        onShare = { shareImage(context, imageUri) },
                        onRetake = handleReturnHome
                    )
                }
            }
        }
    }
}

@Preview(
    name = "Review Screen",
    showBackground = true,
    backgroundColor = 0xFF0E1320,
    widthDp = 390,
    heightDp = 844
)
@Composable
fun ReviewScreenPreview() {
    ReviewScreen(
        selectedFilter = FilterType.CLASSIC_MASK,
        imageUri = null,
        onNavigateToGearSelection = {}
    )
}
