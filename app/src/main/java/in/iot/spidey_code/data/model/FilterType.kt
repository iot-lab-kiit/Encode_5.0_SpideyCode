package `in`.iot.spidey_code.data.model

enum class FilterType {
    CLASSIC_MASK,
    WEB_SHOOTER,
    SPIDEY_SENSE,
    SPIDER_VERSE,
    EVENT_SQUAD,
    SPIDEY_PARTY,
    GHOST_SPIDER
}

/** Which bottom corner of a frame's photo window the shared event badge should straddle. */
enum class BadgeCorner { LEFT, RIGHT }

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
        FilterType.WEB_SHOOTER -> FrameDefinition(
            assetPath = "images/frame_readymade.png",
            fallbackWindow = NormalizedRect(
                left = 0.13542f,
                top = 0.21094f,
                width = 0.72396f,
                height = 0.42188f
            )
        )
        FilterType.SPIDEY_SENSE -> FrameDefinition(
            assetPath = "images/frame_minispider.png",
            fallbackWindow = NormalizedRect(
                left = 0.17188f,
                top = 0.23438f,
                width = 0.65625f,
                height = 0.37793f
            )
        )
        FilterType.SPIDER_VERSE -> FrameDefinition(
            assetPath = "images/frame_spidergirl.png",
            fallbackWindow = NormalizedRect(
                left = 0.24000f,
                top = 0.24000f,
                width = 0.52444f,
                height = 0.31000f
            )
        )
        FilterType.EVENT_SQUAD -> FrameDefinition(
            assetPath = "images/frame_event_squad.png",
            fallbackWindow = NormalizedRect(
                left = 0.06944f,
                top = 0.16113f,
                width = 0.86024f,
                height = 0.43945f
            )
        )
        FilterType.SPIDEY_PARTY -> FrameDefinition(
            assetPath = "images/frame_spidey_party.png",
            fallbackWindow = NormalizedRect(
                left = 0.08000f,
                top = 0.18000f,
                width = 0.84000f,
                height = 0.58000f
            )
        )
        FilterType.GHOST_SPIDER -> FrameDefinition(
            assetPath = "images/frame_ghost_spider.png",
            fallbackWindow = NormalizedRect(
                left = 0.08000f,
                top = 0.16000f,
                width = 0.84000f,
                height = 0.60000f
            )
        )
    }

/**
 * Classic's own source asset (frame_1_2.png) already has AlgoZenith / KIIT / KSAC / IoT
 * logos and the full "ENCODE 5.0 X ZENITHCUP" title baked into its art directly -- so the
 * shared runtime BrandingOverlay must be skipped there to avoid drawing duplicate logos
 * and a second badge on top of it. Every other frame has no baked branding and needs it.
 */
val FilterType.showBrandingOverlay: Boolean
    get() = this != FilterType.CLASSIC_MASK

/** Corner the shared "ENCODE 5.0 X ZENITHCUP" badge straddles on each frame's window,
 *  chosen per-frame to avoid the baked-in spider/web decoration on that same corner. */
val FilterType.badgeCorner: BadgeCorner
    get() = when (this) {
        FilterType.CLASSIC_MASK -> BadgeCorner.RIGHT
        FilterType.WEB_SHOOTER -> BadgeCorner.RIGHT
        FilterType.SPIDEY_SENSE -> BadgeCorner.RIGHT
        FilterType.SPIDER_VERSE -> BadgeCorner.LEFT
        FilterType.EVENT_SQUAD -> BadgeCorner.RIGHT
        FilterType.SPIDEY_PARTY -> BadgeCorner.RIGHT
        FilterType.GHOST_SPIDER -> BadgeCorner.RIGHT
    }

val FilterType.frameAssetPath: String?
    get() = frameDefinition?.assetPath

val FilterType.thumbnailAssetPath: String?
    get() = frameAssetPath

fun FilterType.displayName(): String {
    return when (this) {
        FilterType.CLASSIC_MASK -> "Classic"
        FilterType.WEB_SHOOTER -> "Web Shooter"
        FilterType.SPIDEY_SENSE -> "Spidey Sense"
        FilterType.SPIDER_VERSE -> "Spider-Verse"
        FilterType.EVENT_SQUAD -> "Event Squad"
        FilterType.SPIDEY_PARTY -> "Spidey Party"
        FilterType.GHOST_SPIDER -> "Ghost Spider"
    }
}
