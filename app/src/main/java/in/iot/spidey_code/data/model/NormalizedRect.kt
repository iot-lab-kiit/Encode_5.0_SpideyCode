package `in`.iot.spidey_code.data.model

import android.graphics.Rect
import android.graphics.RectF

/**
 * Represents a normalized bounding rectangle with values in the range [0.0f, 1.0f]
 * relative to an image or container's total width and height.
 */
data class NormalizedRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float
) {
    val right: Float get() = left + width
    val bottom: Float get() = top + height

    fun toPixelRect(imageWidth: Int, imageHeight: Int): Rect {
        return Rect(
            (left * imageWidth).toInt(),
            (top * imageHeight).toInt(),
            (right * imageWidth).toInt(),
            (bottom * imageHeight).toInt()
        )
    }

    fun toPixelRectF(imageWidth: Float, imageHeight: Float): RectF {
        return RectF(
            left * imageWidth,
            top * imageHeight,
            right * imageWidth,
            bottom * imageHeight
        )
    }
}

/**
 * Definition registry data model for a photobooth frame asset.
 */
data class FrameDefinition(
    val assetPath: String,
    val fallbackWindow: NormalizedRect? = null
)
