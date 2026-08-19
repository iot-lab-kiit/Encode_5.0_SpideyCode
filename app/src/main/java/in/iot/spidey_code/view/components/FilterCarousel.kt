package `in`.iot.spidey_code.view.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.data.model.displayName
import `in`.iot.spidey_code.data.model.thumbnailAssetPath
import `in`.iot.spidey_code.ui.theme.SpideyRed
import kotlin.math.abs
import kotlinx.coroutines.launch

private val THUMB_SIZE = 60.dp
private val ITEM_SPACING = 14.dp

/**
 * Snapchat-style horizontal filter switcher for the Camera screen: a center-snapping
 * row of circular filter thumbnails. Tapping one, or swiping until it settles at
 * center, switches the live camera frame without leaving the screen.
 */
@Composable
fun FilterCarousel(
    filters: List<FilterType>,
    selectedFilter: FilterType,
    onFilterSelected: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val sidePadding = ((maxWidth - THUMB_SIZE) / 2).coerceAtLeast(0.dp)

        // Center the initially-selected filter on first composition (no animation).
        LaunchedEffect(Unit) {
            val startIndex = filters.indexOf(selectedFilter).coerceAtLeast(0)
            listState.scrollToItem(startIndex)
        }

        // Once a fling/drag settles, promote whichever thumbnail landed at center.
        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress) {
                val layoutInfo = listState.layoutInfo
                if (layoutInfo.visibleItemsInfo.isNotEmpty()) {
                    val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                    val centeredInfo = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                        abs((item.offset + item.size / 2) - viewportCenter)
                    }
                    val centeredFilter = centeredInfo?.index?.let { filters.getOrNull(it) }
                    if (centeredFilter != null && centeredFilter != selectedFilter) {
                        onFilterSelected(centeredFilter)
                    }
                }
            }
        }

        LazyRow(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(horizontal = sidePadding),
            horizontalArrangement = Arrangement.spacedBy(ITEM_SPACING)
        ) {
            itemsIndexed(filters) { index, filter ->
                FilterThumbnail(
                    filter = filter,
                    isSelected = filter == selectedFilter,
                    onClick = {
                        onFilterSelected(filter)
                        coroutineScope.launch { listState.animateScrollToItem(index) }
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterThumbnail(
    filter: FilterType,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val thumbBitmap = remember(context, filter) {
        filter.thumbnailAssetPath?.let { path ->
            runCatching {
                context.assets.open(path).use { stream ->
                    val options = BitmapFactory.Options().apply {
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                        inPremultiplied = true
                    }
                    BitmapFactory.decodeStream(stream, null, options)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }

    Column(
        modifier = modifier.width(THUMB_SIZE + 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(THUMB_SIZE)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(
                    width = if (isSelected) 3.dp else 1.dp,
                    color = if (isSelected) SpideyRed else Color.White.copy(alpha = 0.3f),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            if (thumbBitmap != null) {
                Image(
                    bitmap = thumbBitmap,
                    contentDescription = filter.displayName(),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Face,
                    contentDescription = filter.displayName(),
                    tint = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = filter.displayName(),
            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.6f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
