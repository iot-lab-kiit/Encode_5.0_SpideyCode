package `in`.iot.spidey_code.view.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition

/**
 * Lottie animation component rendering the spider climbing down/up animation
 * loaded from assets/animations/spider_climbing_down.json.
 */
@Composable
fun SpiderClimbingAnimation(
    modifier: Modifier = Modifier,
    sizeDp: Int = 110
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.Asset("animations/spider_climbing_down.json")
    )

    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier.size(sizeDp.dp)
    )
}
