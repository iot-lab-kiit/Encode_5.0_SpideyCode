package `in`.iot.spidey_code.data.model

enum class FilterType {
    CLASSIC_MASK,
    WEB_SHOOTER,
    SPIDEY_SENSE,
    NONE
}

fun FilterType.displayName(): String {
    return when (this) {
        FilterType.CLASSIC_MASK -> "CLASSIC MASK"
        FilterType.WEB_SHOOTER -> "WEB SHOOTER"
        FilterType.SPIDEY_SENSE -> "SPIDEY SENSE"
        FilterType.NONE -> "NO MASK"
    }
}
