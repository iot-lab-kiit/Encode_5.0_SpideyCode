package `in`.iot.spidey_code.view.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import `in`.iot.spidey_code.data.model.BadgeCorner
import `in`.iot.spidey_code.data.model.NormalizedRect
import `in`.iot.spidey_code.utils.BrandingConfig

private val CORNER_LOGO_ASSETS = listOf(
    "logos/algozenith_logo.png",
    "logos/iot_logo.png",
    "logos/ksac_logo.webp"
)
private const val EVENT_BADGE_ASSET = "logos/encodexzenith_logo.png"

/**
 * Shared branding layer drawn on top of every filter's frame: a small row of society
 * logos (AlgoZenith / IoT / KSAC) in the top-right corner, plus the event badge
 * straddling one bottom corner of the photo window -- half over the photo, half over
 * the frame border. Identical on every filter so club branding never depends on
 * whichever frame art happens to be selected, and applies to the live preview here;
 * ImageCompositionUtils.drawBrandingOverlay mirrors this exact math for the final photo.
 */
@Composable
fun BrandingOverlay(
    normalizedWindow: NormalizedRect?,
    badgeCorner: BadgeCorner,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config = remember(context) { BrandingConfig.load(context) }
    val cornerLogos = remember(context) {
        CORNER_LOGO_ASSETS.mapNotNull { path -> loadAssetBitmap(context, path) }
    }
    val eventBadge = remember(context) { loadAssetBitmap(context, EVENT_BADGE_ASSET) }

    BoxWithConstraints(modifier = modifier) {
        val logoSize = maxWidth * config.logoSizeRatio
        val logoGap = maxWidth * config.logoGapRatio
        val logoMarginTop = maxHeight * config.logoMarginTopRatio
        val logoMarginEnd = maxWidth * config.logoMarginEndRatio

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = logoMarginTop, end = logoMarginEnd),
        ) {
            cornerLogos.forEach { logo ->
                Image(
                    bitmap = logo,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .padding(start = logoGap)
                        .size(logoSize)
                        .clip(CircleShape)
                )
            }
        }

        if (normalizedWindow != null && eventBadge != null) {
            val badgeWidth = maxWidth * config.badgeWidthRatio
            val aspect = eventBadge.width.toFloat() / eventBadge.height.toFloat()
            val badgeHeight = badgeWidth / aspect

            val cornerX = maxWidth * (if (badgeCorner == BadgeCorner.RIGHT) normalizedWindow.right else normalizedWindow.left)
            val cornerY = maxHeight * normalizedWindow.bottom
            val offsetX: Dp = if (badgeCorner == BadgeCorner.RIGHT) {
                cornerX - badgeWidth * config.badgeInsetRatio
            } else {
                cornerX - badgeWidth * (1f - config.badgeInsetRatio)
            }
            val offsetY: Dp = cornerY - badgeHeight * 0.5f

            Image(
                bitmap = eventBadge,
                contentDescription = null,
                modifier = Modifier
                    .absoluteOffset(x = offsetX, y = offsetY)
                    .size(badgeWidth, badgeHeight)
            )
        }
    }
}

private fun loadAssetBitmap(context: android.content.Context, path: String): ImageBitmap? {
    return runCatching {
        context.assets.open(path).use { stream ->
            val options = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inPremultiplied = true
            }
            BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
        }
    }.getOrNull()
}
