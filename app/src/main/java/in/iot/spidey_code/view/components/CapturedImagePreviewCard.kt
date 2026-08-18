package `in`.iot.spidey_code.view.components

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import `in`.iot.spidey_code.ui.theme.SpiderBorderBlack
import `in`.iot.spidey_code.ui.theme.SpiderPrimaryContainer
import `in`.iot.spidey_code.ui.theme.SpiderSurfaceContainer

import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.remember

@Composable
fun CapturedImagePreviewCard(
    capturedBitmap: Bitmap?,
    imageUri: String?,
    modifier: Modifier = Modifier
) {
    val imageAspectRatio = remember(capturedBitmap) {
        if (capturedBitmap != null && capturedBitmap.height > 0) {
            capturedBitmap.width.toFloat() / capturedBitmap.height.toFloat()
        } else {
            9f / 16f
        }
    }

    BrutalistBox(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(imageAspectRatio),
        shape = RoundedCornerShape(8.dp),
        bg = SpiderSurfaceContainer,
        borderColor = SpiderBorderBlack,
        borderWidth = 4.dp,
        shadowOffset = 4.dp
    ) {
        if (capturedBitmap != null) {
            Image(
                bitmap = capturedBitmap.asImageBitmap(),
                contentDescription = "Captured Photo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else if (imageUri != null) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = SpiderPrimaryContainer
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(SpiderSurfaceContainer)
            )
        }

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
