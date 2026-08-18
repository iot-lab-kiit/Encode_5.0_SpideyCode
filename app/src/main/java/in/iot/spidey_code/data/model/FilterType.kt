package `in`.iot.spidey_code.data.model

enum class FilterType {
    CLASSIC_MASK,
    WEB_SHOOTER,
    SPIDEY_SENSE,
    NONE
}

val FilterType.frameDefinition: FrameDefinition?
    get() = when (this) {
        FilterType.CLASSIC_MASK -> FrameDefinition(
            assetPath = "images/frame_1_2.png",
            fallbackWindow = NormalizedRect(
                left = 0.05185f,
                top = 0.11146f,
                width = 0.87963f,
                height = 0.49583f
            )
        )
        else -> null
    }

val FilterType.frameAssetPath: String?
    get() = frameDefinition?.assetPath

fun FilterType.displayName(): String {
    return when (this) {
        FilterType.CLASSIC_MASK -> "Classic"
        FilterType.WEB_SHOOTER -> "WEB SHOOTER"
        FilterType.SPIDEY_SENSE -> "SPIDEY SENSE"
        FilterType.NONE -> "NO MASK"
    }
}

