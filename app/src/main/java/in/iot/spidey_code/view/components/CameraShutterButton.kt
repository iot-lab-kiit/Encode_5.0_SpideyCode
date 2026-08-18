package `in`.iot.spidey_code.view.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import `in`.iot.spidey_code.ui.theme.SpideyRed
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** How long a press must be held before it's treated as "start recording" instead of a tap. */
private const val HOLD_THRESHOLD_MS = 220L

/**
 * Snapchat-style shutter: a quick tap takes a photo, a press-and-hold starts video
 * recording (continues for as long as the button is held) and releasing stops it.
 * While recording, an animated ring around the button fills to show elapsed progress
 * toward the max clip duration.
 */
@Composable
fun CameraShutterButton(
    onTapPhoto: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isCapturing: Boolean = false,
    isRecording: Boolean = false,
    recordingProgress: Float = 0f,
    sizeDp: Int = 76
) {
    val coroutineScope = rememberCoroutineScope()
    val ringDiameter = sizeDp + 24

    Box(
        modifier = modifier
            .size(ringDiameter.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        var startedRecording = false
                        val holdJob = coroutineScope.launch {
                            delay(HOLD_THRESHOLD_MS)
                            startedRecording = true
                            onStartRecording()
                        }
                        val released = tryAwaitRelease()
                        if (startedRecording) {
                            onStopRecording()
                        } else {
                            holdJob.cancel()
                            if (released) onTapPhoto()
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (isRecording) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 5.dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = 0.25f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = SpideyRed,
                    startAngle = -90f,
                    sweepAngle = 360f * recordingProgress.coerceIn(0f, 1f),
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(sizeDp.dp)
                .clip(CircleShape)
                .border(BorderStroke(4.dp, if (isRecording) SpideyRed else Color.White), CircleShape)
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        when {
                            isRecording -> SpideyRed
                            isCapturing -> Color.LightGray
                            else -> Color.White
                        }
                    )
            )
        }
    }
}
