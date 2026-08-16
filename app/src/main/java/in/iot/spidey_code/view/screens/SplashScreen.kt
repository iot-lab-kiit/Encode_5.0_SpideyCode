package `in`.iot.spidey_code.view.screens

import android.graphics.BitmapFactory
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import `in`.iot.spidey_code.ui.theme.DarkBackground
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var startAnimation by remember { mutableStateOf(false) }

    val splashBitmap = remember {
        runCatching {
            context.assets.open("images/splash_screen_3.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.recoverCatching {
            context.assets.open("images/splash_screen_3.png").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "alpha"
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.92f,
        animationSpec = tween(
            durationMillis = 800,
            easing = FastOutSlowInEasing
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        startAnimation = true
        delay(1800)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        if (splashBitmap != null) {
            Image(
                bitmap = splashBitmap.asImageBitmap(),
                contentDescription = "SpideyCode Splash Screen",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth(0.92f)
                    .alpha(alphaAnim)
                    .scale(scaleAnim)
            )
        }
    }
}
