package `in`.iot.spidey_code.view.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import `in`.iot.spidey_code.ui.theme.SpiderBorderBlack
import `in`.iot.spidey_code.ui.theme.SpiderPrimaryContainer
import `in`.iot.spidey_code.ui.theme.SpiderSurfaceContainer

/**
 * Video counterpart of CapturedImagePreviewCard: same brutalist-framed card treatment, but
 * hosting an ExoPlayer surface instead of a static bitmap.
 */
@Composable
fun VideoPreviewCard(
    exoPlayer: ExoPlayer,
    modifier: Modifier = Modifier
) {
    var videoAspectRatio by remember { mutableFloatStateOf(9f / 16f) }

    androidx.compose.runtime.DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoAspectRatio = (videoSize.width * videoSize.pixelWidthHeightRatio) / videoSize.height
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    BrutalistBox(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(videoAspectRatio),
        shape = RoundedCornerShape(8.dp),
        bg = SpiderSurfaceContainer,
        borderColor = SpiderBorderBlack,
        borderWidth = 4.dp,
        shadowOffset = 4.dp
    ) {
        AndroidView(
            factory = { context ->
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        Canvas(
            modifier = Modifier
                .align(Alignment.TopStart)
                .size(44.dp)
        ) {
            val stroke = 8.dp.toPx()
            val len = size.width
            drawLine(
                color = SpiderPrimaryContainer,
                start = Offset(0f, stroke / 2f),
                end = Offset(len, stroke / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Square
            )
            drawLine(
                color = SpiderPrimaryContainer,
                start = Offset(stroke / 2f, 0f),
                end = Offset(stroke / 2f, len),
                strokeWidth = stroke,
                cap = StrokeCap.Square
            )
        }

        Canvas(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(44.dp)
        ) {
            val stroke = 8.dp.toPx()
            val len = size.width
            drawLine(
                color = SpiderPrimaryContainer,
                start = Offset(0f, size.height - stroke / 2f),
                end = Offset(len, size.height - stroke / 2f),
                strokeWidth = stroke,
                cap = StrokeCap.Square
            )
            drawLine(
                color = SpiderPrimaryContainer,
                start = Offset(size.width - stroke / 2f, size.height - len),
                end = Offset(size.width - stroke / 2f, size.height),
                strokeWidth = stroke,
                cap = StrokeCap.Square
            )
        }
    }
}
