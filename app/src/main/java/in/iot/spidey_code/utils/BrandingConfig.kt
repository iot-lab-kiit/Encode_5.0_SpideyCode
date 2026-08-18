package `in`.iot.spidey_code.utils

import android.content.Context
import org.json.JSONObject

/**
 * Sizing/positioning for the shared corner-logo + event-badge overlay (see
 * BrandingOverlay.kt for live preview, ImageCompositionUtils.drawBrandingOverlay
 * for the final photo). Loaded from app/assets/branding.json at runtime so it's
 * editable directly -- no code changes or build script needed, just edit the
 * JSON and rebuild the app.
 */
data class BrandingConfig(
    val logoSizeRatio: Float,
    val logoGapRatio: Float,
    val logoMarginTopRatio: Float,
    val logoMarginEndRatio: Float,
    val badgeWidthRatio: Float,
    val badgeInsetRatio: Float
) {
    companion object {
        val DEFAULT = BrandingConfig(
            logoSizeRatio = 0.10f,
            logoGapRatio = 0.012f,
            logoMarginTopRatio = 0.025f,
            logoMarginEndRatio = 0.025f,
            badgeWidthRatio = 0.34f,
            badgeInsetRatio = 0.65f
        )

        fun load(context: Context): BrandingConfig {
            return runCatching {
                context.assets.open("branding.json").use { stream ->
                    val json = JSONObject(stream.bufferedReader().readText())
                    BrandingConfig(
                        logoSizeRatio = json.optDouble("logoSizeRatio", DEFAULT.logoSizeRatio.toDouble()).toFloat(),
                        logoGapRatio = json.optDouble("logoGapRatio", DEFAULT.logoGapRatio.toDouble()).toFloat(),
                        logoMarginTopRatio = json.optDouble("logoMarginTopRatio", DEFAULT.logoMarginTopRatio.toDouble()).toFloat(),
                        logoMarginEndRatio = json.optDouble("logoMarginEndRatio", DEFAULT.logoMarginEndRatio.toDouble()).toFloat(),
                        badgeWidthRatio = json.optDouble("badgeWidthRatio", DEFAULT.badgeWidthRatio.toDouble()).toFloat(),
                        badgeInsetRatio = json.optDouble("badgeInsetRatio", DEFAULT.badgeInsetRatio.toDouble()).toFloat()
                    )
                }
            }.getOrDefault(DEFAULT)
        }
    }
}
