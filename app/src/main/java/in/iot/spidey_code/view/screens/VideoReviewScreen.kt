package `in`.iot.spidey_code.view.screens

import android.graphics.BitmapFactory
import android.net.Uri
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.data.model.displayName
import `in`.iot.spidey_code.ui.theme.SpiderSurface
import `in`.iot.spidey_code.utils.saveVideoToGallery
import `in`.iot.spidey_code.utils.shareVideo
import `in`.iot.spidey_code.view.components.ReviewActionRow
import `in`.iot.spidey_code.view.components.ReviewTopBar
import `in`.iot.spidey_code.view.components.VideoPreviewCard
import java.io.File

@Composable
fun VideoReviewScreen(
    selectedFilter: FilterType,
    videoUri: String?,
    onNavigateBack: () -> Unit,
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

    val exoPlayer = remember(videoUri) {
        ExoPlayer.Builder(context).build().apply {
            if (videoUri != null) {
                setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
                repeatMode = Player.REPEAT_MODE_ONE
                prepare()
                playWhenReady = true
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose { exoPlayer.release() }
    }

    val handleReturnHome = {
        if (videoUri != null) {
            runCatching {
                val parsedUri = Uri.parse(videoUri)
                if (parsedUri.scheme == "file" && parsedUri.path != null) {
                    val file = File(parsedUri.path!!)
                    if (file.exists()) file.delete()
                }
            }
        }
        onNavigateBack()
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
            ReviewTopBar(
                filterName = selectedFilter.displayName(),
                onBack = handleReturnHome
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                VideoPreviewCard(exoPlayer = exoPlayer)
            }

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
                        onDownload = { saveVideoToGallery(context, videoUri) },
                        onShare = { shareVideo(context, videoUri) },
                        onRetake = handleReturnHome
                    )
                }
            }
        }
    }
}
