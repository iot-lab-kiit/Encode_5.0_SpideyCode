package `in`.iot.spidey_code.view.screens

import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import `in`.iot.spidey_code.data.model.FilterType
import `in`.iot.spidey_code.data.model.displayName
import `in`.iot.spidey_code.ui.theme.SpiderSurface
import `in`.iot.spidey_code.view.components.ContinueButton
import `in`.iot.spidey_code.view.components.FilterCard
import `in`.iot.spidey_code.view.components.GearSelectionHeader
import `in`.iot.spidey_code.view.components.SpiderClimbingAnimation
import `in`.iot.spidey_code.vm.GearSelectionViewModel

@Composable
fun GearSelectionScreen(
    onNavigateToCamera: (FilterType) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GearSelectionViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedFilter by viewModel.selectedFilter.collectAsState()

    val backgroundBitmap = remember {
        runCatching {
            context.assets.open("images/spider_verse_bg.jpg").use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrNull()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SpiderSurface)
    ) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                colorFilter = ColorFilter.tint(
                    SpiderSurface,
                    BlendMode.Multiply
                ),
                modifier = Modifier.fillMaxSize()
            )
        }

        Canvas(Modifier.fillMaxSize()) {
            val step = 20.dp.toPx()
            for (x in 0..(size.width / step).toInt()) {
                for (y in 0..(size.height / step).toInt()) {
                    drawCircle(Color(0x0DFFFFFF), 1.dp.toPx(), Offset(x * step, y * step))
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            GearSelectionHeader()

            Spacer(modifier = Modifier.height(6.dp))

            // Scrollable 2-column grid -- scales cleanly as more filters are added,
            // unlike a hand-paired Row/Column layout (7 filters doesn't split evenly).
            fun selectAndGo(filter: FilterType) {
                viewModel.selectFilter(filter)
                onNavigateToCamera(filter)
            }

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(FilterType.entries.toList(), key = { it.name }) { filter ->
                    FilterCard(
                        filterType = filter,
                        title = filter.displayName(),
                        isSelected = selectedFilter == filter,
                        onSelect = { selectAndGo(filter) },
                        isClickable = true,
                        previewBackgroundColor = if (filter == FilterType.CLASSIC_MASK) {
                            `in`.iot.spidey_code.ui.theme.SpideyRed.copy(alpha = 0.25f)
                        } else {
                            null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.9f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            ContinueButton(
                text = "CONTINUE TO CAMERA",
                onClick = { onNavigateToCamera(selectedFilter) }
            )
        }

        // TOP RIGHT CORNER: Spider Climbing Down Animation hanging from top edge
        SpiderClimbingAnimation(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 8.dp),
            sizeDp = 110
        )
    }
}
